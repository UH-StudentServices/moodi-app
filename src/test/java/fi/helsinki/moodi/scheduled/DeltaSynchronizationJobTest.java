/*
 * This file is part of Moodi application.
 *
 * Moodi application is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moodi application is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Moodi application.  If not, see <http://www.gnu.org/licenses/>.
 */

package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.exception.SynchronizationInProgressException;
import fi.helsinki.moodi.integration.moodle.MoodleCourseWithEnrollments;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem.UserSynchronizationItemStatus;
import fi.helsinki.moodi.service.synchronize.snapshot.CourseSyncParticipant;
import fi.helsinki.moodi.test.util.DateUtil;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static fi.helsinki.moodi.test.util.DateUtil.getPastDateString;
import static org.junit.Assert.*;

/**
 * DELTA synchronization is delta-based: actions are resolved against the locally stored
 * snapshot of the last synced Sisu state, and the Moodle enrollment state is not fetched
 * for bootstrapped courses. Courses whose snapshots have never been seeded
 * (sync_bootstrapped = false) are automatically reconciled against the live Moodle state
 * as part of the run.
 */
@TestPropertySource(properties = {"syncTresholds.REMOVE_ROLE.preventAll = 0"})
public class DeltaSynchronizationJobTest extends AbstractSynchronizationJobTest {

    private void synchronizeBootstrappedCourseWithNoSnapshotRows(String endDateString) {
        // Bootstrapped course with no snapshot rows: all Sisu users are new and get
        // delta ADD_ENROLLMENT actions.
        markCourseBootstrapped();

        setUpMockServerResponses(endDateString, true);

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatCourseIsSynchronizedWhenEndDateInFuture() {
        synchronizeBootstrappedCourseWithNoSnapshotRows(getFutureDateString());
    }

    @Test
    public void thatSnapshotIsWrittenForNewUsersAfterSuccessfulSync() {
        synchronizeBootstrappedCourseWithNoSnapshotRows(getFutureDateString());

        CourseSyncParticipant studentSnapshot = getSnapshot(MOODLE_USERNAME_NIINA);
        assertTrue(studentSnapshot.student);
        assertTrue(studentSnapshot.enrolledStudent);
        assertFalse(studentSnapshot.teacher);
        assertEquals((Long) MOODLE_USER_ID_NIINA, studentSnapshot.moodleUserId);

        CourseSyncParticipant teacherSnapshot = getSnapshot(MOODLE_USERNAME_HRAOPE);
        assertFalse(teacherSnapshot.student);
        assertTrue(teacherSnapshot.teacher);
        assertEquals((Long) MOODLE_USER_HRAOPE, teacherSnapshot.moodleUserId);
    }

    @Test
    public void thatNonBootstrappedCourseIsAutoReconciledOnDeltaRun() {
        assertFalse(findCourse().syncBootstrapped);

        setUpMockServerResponses(getFutureDateString(), true);

        // The course is stamped RECONCILE within the delta run, so the live Moodle
        // enrollment state is fetched and the Sisu users are enrolled against it.
        prepareMoodleGetEnrolledUsersForCoursesMock(MOODLE_COURSE_ID_IN_DB, Collections.emptyList());

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();

        CourseSyncParticipant studentSnapshot = getSnapshot(MOODLE_USERNAME_NIINA);
        assertTrue(studentSnapshot.student);
        assertTrue(studentSnapshot.enrolledStudent);
        assertEquals((Long) MOODLE_USER_ID_NIINA, studentSnapshot.moodleUserId);

        CourseSyncParticipant teacherSnapshot = getSnapshot(MOODLE_USERNAME_HRAOPE);
        assertTrue(teacherSnapshot.teacher);

        assertTrue(findCourse().syncBootstrapped);
    }

    @Test
    public void thatSecondRunAfterAutoReconcileIsPureDelta() {
        thatNonBootstrappedCourseIsAutoReconciledOnDeltaRun();

        verifyMockServers();
        setUpMockServers();

        // The bootstrapped and unchanged course generates no enrollment fetches, user
        // lookups or enrollment requests (enforced by the mock server verification in the
        // @After hook).
        setUpMockServerResponses(getFutureDateString(), true);

        job.execute();

        assertTrue(findCourse().syncBootstrapped);
    }

    @Test
    public void thatCourseIsNotMarkedBootstrappedWhenAutoReconcileFails() {
        setUpMockServerResponses(getFutureDateString(), true);

        // Both the batched enrollment fetch and the per-course fallback fail, so the
        // auto-reconcile item errors during enrichment.
        prepareMoodleGetEnrolledUsersForCoursesMock(
            Collections.singletonList(new MoodleCourseWithEnrollments(MOODLE_COURSE_ID_IN_DB, Collections.emptyList())), 0);

        job.execute();

        // The course stays non-bootstrapped and is auto-reconciled again on the next run.
        assertFalse(findCourse().syncBootstrapped);
        assertNull(getSnapshot(MOODLE_USERNAME_NIINA));
    }

    @Test
    public void thatUnchangedCourseGeneratesNoMoodleRequests() {
        setUpMockServerResponses(getFutureDateString(), true);

        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, true, false);
        seedSnapshot(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE, false, false, true);

        // No expectations besides the course prefetch: no user lookups, no enrollment requests
        // (enforced by the mock server verification in the @After hook).
        SynchronizationSummary summary = synchronizationService.synchronize(SynchronizationType.DELTA);

        assertEquals(SynchronizationStatus.COMPLETED_SUCCESS, summary.getStatus());
        assertTrue(summary.getItems().getFirst().getUserSynchronizationItems().stream()
            .allMatch(item -> item.getActions().isEmpty()));
    }

    @Test
    public void thatOverYearOldCourseIsRemovedAndSnapshotIsCleanedUp() {
        String endDateInPast = DateUtil.getOverYearAgoPastDateString();
        setupCourseUnitRealisationResponse(endDateInPast, true);

        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, true, false);

        Course course = findCourse();

        assertFalse(course.removed);

        job.execute();

        course = findCourse();

        assertTrue(course.removed);
        assertEquals(course.removedMessage, EnrichmentStatus.COURSE_ENDED.toString());
        assertNull(getSnapshot(MOODLE_USERNAME_NIINA));
        // The bootstrap flag follows the snapshots: a resurrected course starts non-bootstrapped.
        assertFalse(course.syncBootstrapped);
    }

    @Test
    public void thatEndedCourseIsStillSyncedIfLessThanYearHasPassed() {

        Course course = findCourse();

        assertFalse(course.removed);

        synchronizeBootstrappedCourseWithNoSnapshotRows(getPastDateString());

        course = findCourse();

        assertFalse(course.removed);
    }

    @Test
    public void thatImportStatusIsSetToCompletedAndEnrollmentStatusesAreGenerated() {
        assertImportStatus(ImportStatus.COMPLETED_FAILED);

        thatCourseIsSynchronizedWhenEndDateInFuture();

        assertImportStatus(ImportStatus.COMPLETED);
    }

    @Test
    public void thatNonEnrolledStudentIsNotEnrolledToMoodle() {
        markCourseBootstrapped();

        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        // Only the teacher has a pending action, so only the teacher is looked up from Moodle.
        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();

        // The non-enrolled student is recorded in the snapshot without any Moodle action.
        CourseSyncParticipant studentSnapshot = getSnapshot(MOODLE_USERNAME_NIINA);
        assertTrue(studentSnapshot.student);
        assertFalse(studentSnapshot.enrolledStudent);
    }

    @Test
    public void thatNewTeacherRoleIsAddedForAlreadySyncedStudent() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, true);

        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, true, false);

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatNonEnrolledStudentIsSuspendedAndRoleRemovedFromMoodle() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, true, false);
        seedSnapshot(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE, false, false, true);

        // The Moodle user id comes from the snapshot, so no user lookups are needed.
        expectSuspendRequestToMoodle(new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodle(false, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        job.execute();

        CourseSyncParticipant studentSnapshot = getSnapshot(MOODLE_USERNAME_NIINA);
        assertFalse(studentSnapshot.enrolledStudent);
    }

    @Test
    public void thatAlreadySuspendedStudentIsNotSuspendedAgain() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, false, false);
        seedSnapshot(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE, false, false, true);

        // No expectations: the suspension has already been synced to Moodle
        // (enforced by the mock server verification in the @After hook).
        SynchronizationSummary summary = synchronizationService.synchronize(SynchronizationType.DELTA);

        assertEquals(SynchronizationStatus.COMPLETED_SUCCESS, summary.getStatus());
        assertTrue(summary.getItems().getFirst().getUserSynchronizationItems().stream()
            .allMatch(item -> item.getActions().isEmpty()));
    }

    @Test
    public void thatReEnrolledStudentIsReactivatedInMoodle() {
        String endDateInFuture = getFutureDateString();
        // Student enrolled in Sisu...
        setUpMockServerResponses(endDateInFuture, true);

        // ...but the snapshot says the student was synced as not enrolled.
        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, false, false);
        seedSnapshot(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE, false, false, true);

        // Re-enrolling clears the suspension in Moodle.
        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        job.execute();

        CourseSyncParticipant studentSnapshot = getSnapshot(MOODLE_USERNAME_NIINA);
        assertTrue(studentSnapshot.enrolledStudent);
    }

    @Test
    public void thatFailedEnrollmentIsRetriedOnNextRun() {
        markCourseBootstrapped();

        setUpMockServerResponses(getFutureDateString(), true);

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodleWithResponse(MOODLE_ERROR_RESPONSE,
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();

        // No snapshot was written for the failed users...
        assertNull(getSnapshot(MOODLE_USERNAME_NIINA));
        assertNull(getSnapshot(MOODLE_USERNAME_HRAOPE));

        // ...so the next run resolves the same delta and retries. Moodle user ids are
        // served from the cache, so no new user lookups are made.
        verifyMockServers();
        setUpMockServers();
        setUpMockServerResponses(getFutureDateString(), true);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();

        assertNotNull(getSnapshot(MOODLE_USERNAME_NIINA));
        assertNotNull(getSnapshot(MOODLE_USERNAME_HRAOPE));
    }

    @Test
    public void thatMultipleStudentsAndTeachersAreSyncedCorrectly() {
        markCourseBootstrapped();

        prepareMoodleGetCoursesResponseMock();

        setupCourseUnitRealisationResponseMultiplePeople();

        expectFindStudentRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectFindStudentRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA);
        expectFindStudentRequestToMoodle(MOODLE_USERNAME_MAKE, MOODLE_USER_ID_MAKE);

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_ONE, MOODLE_USER_TEACH_ONE);
        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_TWO, MOODLE_USER_TEACH_TWO);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),

            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID_IN_DB),

            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_MAKE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_MAKE, MOODLE_COURSE_ID_IN_DB),

            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_TEACH_ONE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_TEACH_ONE, MOODLE_COURSE_ID_IN_DB),

            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_TEACH_TWO, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_TEACH_TWO, MOODLE_COURSE_ID_IN_DB)
        );

        job.execute();
    }

    @Test
    public void thatUserSyncedByMoodiButRemovedFromSisuIsSuspended() {
        prepareMoodleGetCoursesResponseMock();

        setupCourseUnitRealisationResponse(getFutureDateString(), true);

        // A user that was synced to Moodle earlier no longer appears in Sisu at all.
        seedSnapshot(MOODLE_USERNAME_NOT_ENROLLED_IN_SISU, MOODLE_USER_NOT_ENROLLED_IN_SISU, true, true, false);
        // The current Sisu users are already in sync.
        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, true, false);
        seedSnapshot(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE, false, false, true);

        expectSuspendRequestToMoodle(new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_NOT_ENROLLED_IN_SISU, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodle(false, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_NOT_ENROLLED_IN_SISU, MOODLE_COURSE_ID_IN_DB));

        job.execute();

        CourseSyncParticipant removedSnapshot = getSnapshot(MOODLE_USERNAME_NOT_ENROLLED_IN_SISU);
        assertFalse(removedSnapshot.student);
        assertFalse(removedSnapshot.enrolledStudent);
    }

    @Test
    public void thatSynchronizationSummaryContainsErrorStatusesAndMessagesWhenMoodleResponseWithError() {
        testSynchronizationSummary(SynchronizationType.DELTA, MOODLE_ERROR_RESPONSE, true);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponseWithEmptyString() {
        testSynchronizationSummary(SynchronizationType.DELTA, EMPTY_RESPONSE, false);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponseWithNull() {
        testSynchronizationSummary(SynchronizationType.DELTA, MOODLE_NULL_OK_RESPONSE, false);
    }

    @Test
    public void thatSynchronizationIsNotStartedIfAlreadyInProgress() {
        boolean exceptionThrown = false;

        synchronizationJobRunService.begin(SynchronizationType.DELTA);

        try {
            job.execute();
        } catch (SynchronizationInProgressException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }

    @Test
    public void thatIfUserIsNotFoundFromMoodleSynchronizationIsNotCompleted() {
        markCourseBootstrapped();

        prepareMoodleGetCoursesResponseMock();

        setupCourseUnitRealisationResponse(getFutureDateString(), true);

        expectGetUserRequestToMoodleUserNotFound(MOODLE_USERNAME_NIINA);
        expectGetUserRequestToMoodleUserNotFound(MOODLE_USERNAME_HRAOPE);

        SynchronizationSummary summary = synchronizationService.synchronize(SynchronizationType.DELTA);

        UserSynchronizationItem userSynchronizationItem = summary.getItems().getFirst().getUserSynchronizationItems().getFirst();

        assertEquals(UserSynchronizationItemStatus.MOODLE_USER_NOT_FOUND, userSynchronizationItem.getStatus());

        // Users missing from Moodle get no snapshot row, so they are retried on every run.
        assertNull(getSnapshot(MOODLE_USERNAME_NIINA));
        assertNull(getSnapshot(MOODLE_USERNAME_HRAOPE));
    }
}
