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
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem.UserSynchronizationItemStatus;
import fi.helsinki.moodi.test.util.DateUtil;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static fi.helsinki.moodi.test.util.DateUtil.getPastDateString;
import static org.junit.Assert.*;

@TestPropertySource(properties = {"syncTresholds.REMOVE_ROLE.preventAll = 0"})
public class FullSynchronizationJobTest extends AbstractSynchronizationJobTest {

    private void thatCourseIsSynchronizedWithNoExistingEnrollments(String endDateString) {
        setUpMockServerResponses(endDateString, true);

        prepareMoodleGetEnrolledUsersForCoursesMock(MOODLE_COURSE_ID_IN_DB, Collections.emptyList());

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
        thatCourseIsSynchronizedWithNoExistingEnrollments(getFutureDateString());
    }

    @Test
    public void thatOverYearOldCourseIsRemoved() {
        String endDateInPast = DateUtil.getOverYearAgoPastDateString();
        setupCourseUnitRealisationResponse(endDateInPast, true);

        Course course = findCourse();

        assertFalse(course.removed);

        job.execute();

        course = findCourse();

        assertTrue(course.removed);
        assertEquals(course.removedMessage, EnrichmentStatus.COURSE_ENDED.toString());
    }

    @Test
    public void thatEndedCourseIsStillSyncedIfLessThanYearHasPassed() {

        Course course = findCourse();

        assertFalse(course.removed);

        thatCourseIsSynchronizedWithNoExistingEnrollments(getPastDateString());

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
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        prepareMoodleGetEnrolledUsersForCoursesMock(MOODLE_COURSE_ID_IN_DB, Collections.emptyList());

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatEnrolledStudentIsAssignedStudentRoleInMoodleIfAlreadyEnrolledForAnotherRole() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, true);

        prepareMoodleGetEnrolledUsersForCoursesMock(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA, (int) MOODLE_COURSE_ID_IN_DB,
                    mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodle(true, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatNonEnrolledStudentIsSuspendedAndRoleRemovedFromMoodle() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        prepareMoodleGetEnrolledUsersForCoursesMock(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA, (int) MOODLE_COURSE_ID_IN_DB,
                    mapperService.getStudentRoleId(), mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        expectSuspendRequestToMoodle(new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodle(false, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatReEnrolledStudentIsEnrolledAndRoleAddedInMoodle() {
        String endDateInFuture = getFutureDateString();
        // Student enrolled in Sisu...
        setUpMockServerResponses(endDateInFuture, true);

        // ...but only has the sync role in Moodle.
        prepareMoodleGetEnrolledUsersForCoursesMock(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA,
                    (int) MOODLE_COURSE_ID_IN_DB, mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodle(true, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatUserWithSyncRoleIsNotSuspendedIfDoesNotSeeCourse() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        prepareMoodleGetEnrolledUsersForCoursesMock(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA,
                    (int) SOME_OTHER_MOODLE_COURSE_ID, mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatMultipleStudentsAndTeachersAreSyncedCorrectly() {

        prepareMoodleGetCoursesResponseMock();

        setupCourseUnitRealisationResponseMultiplePeople();

        prepareMoodleGetEnrolledUsersForCoursesMock(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_NOT_ENROLLED_IN_SISU, MOODLE_USERNAME_NOT_ENROLLED_IN_SISU,
                    (int) MOODLE_COURSE_ID_IN_DB, mapperService.getStudentRoleId(), mapperService.getMoodiRoleId())
            )
        );

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

        expectSuspendRequestToMoodle(new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_NOT_ENROLLED_IN_SISU, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodle(false, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_NOT_ENROLLED_IN_SISU, MOODLE_COURSE_ID_IN_DB));

        job.execute();
    }

    @Test
    public void thatSynchronizationSummaryContainsErrorStatusesAndMessagesWhenMoodleResponseWithError() {
        testSynchronizationSummary(SynchronizationType.FULL, MOODLE_ERROR_RESPONSE, true);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponseWithEmptyString() {
        testSynchronizationSummary(SynchronizationType.FULL, EMPTY_RESPONSE, false);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponseWithNull() {
        testSynchronizationSummary(SynchronizationType.FULL, MOODLE_NULL_OK_RESPONSE, false);
    }

    @Test
    public void thatSynchronizationIsNotStartedIfAlreadyInProgress() {
        boolean exceptionThrown = false;

        synchronizationJobRunService.begin(SynchronizationType.FULL);

        try {
            job.execute();
        } catch (SynchronizationInProgressException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }

    @Test
    public void thatIfUserIsNotFoundFromMoodleSynchronizationIsNotCompleted() {
        prepareMoodleGetCoursesResponseMock();

        setupCourseUnitRealisationResponse(getFutureDateString(), true);

        prepareMoodleGetEnrolledUsersForCoursesMock(MOODLE_COURSE_ID_IN_DB, Collections.emptyList());

        expectGetUserRequestToMoodleUserNotFound(MOODLE_USERNAME_NIINA);
        expectGetUserRequestToMoodleUserNotFound(MOODLE_USERNAME_HRAOPE);

        SynchronizationSummary summary = synchronizationService.synchronize(SynchronizationType.FULL);

        UserSynchronizationItem userSynchronizationItem = summary.getItems().get(0).getUserSynchronizationItems().get(0);

        assertEquals(UserSynchronizationItemStatus.MOODLE_USER_NOT_FOUND, userSynchronizationItem.getStatus());
    }
}
