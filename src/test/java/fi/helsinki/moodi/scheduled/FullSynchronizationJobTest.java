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

import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.test.util.DateUtil;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static fi.helsinki.moodi.test.util.DateUtil.getPastDateString;
import static org.junit.Assert.*;

@TestPropertySource(properties = {"syncTresholds.REMOVE_ROLE.preventAll = 0"})
public class FullSynchronizationJobTest extends AbstractSynchronizationJobTest {

    private void thatCourseIsSynchronizedWithNoExistingEnrollments(String endDateString) {
        setUpMockServerResponses(endDateString, true);

        expectGetEnrollmentsRequestToMoodle(MOODLE_COURSE_ID);

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    @Test
    public void thatCourseIsSynchronizedWhenEndDateInFuture() {
        thatCourseIsSynchronizedWithNoExistingEnrollments(getFutureDateString());
    }

    @Test
    public void thatOverYearOldCourseIsRemoved() {
        String endDateInPast = DateUtil.getOverYearAgoPastDateString();
        setupOodiCourseUnitRealisationResponse(endDateInPast, true);

        Course course = findCourse();

        assertFalse(course.removed);

        job.execute();

        course = findCourse();

        assertTrue(course.removed);
        assertEquals(course.removedMessage, EnrichmentStatus.OODI_COURSE_ENDED.toString());
    }

    @Test
    public void thatEndedCourseIsStillSynchedIfLessThanYearHasPassed() {

        Course course = findCourse();

        assertFalse(course.removed);

        thatCourseIsSynchronizedWithNoExistingEnrollments(getPastDateString());

        course = findCourse();

        assertFalse(course.removed);
    }

    @Test
    public void thatImportStatusIsSetToCompletedAndEnrollmentStatusesAreGenerated() {
        assertImportStatus(ImportStatus.COMPLETED_FAILED);

        assertNull(courseEnrollmentStatusService.getCourseEnrollmentStatus(REALISATION_ID));

        thatCourseIsSynchronizedWhenEndDateInFuture();

        assertNotNull(courseEnrollmentStatusService.getCourseEnrollmentStatus(REALISATION_ID));

        assertImportStatus(ImportStatus.COMPLETED);
    }

    @Test
    public void thatUnApprovedStudentIsNotEnrolledToMoodle() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        expectGetEnrollmentsRequestToMoodle(MOODLE_COURSE_ID);

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    @Test
    public void thatApprovedStudentIsAssignedStudentRoleInMoodleIfAlreadyEnrolledForAnotherRole() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, true);

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()));

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        expectAssignRolesToMoodle(true, new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    @Test
    public void thatUnApprovedStudentHasStudentRoleUnassignedFromMoodle() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, false);

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()));

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        expectAssignRolesToMoodle(false,
            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    @Test
    public void thatMultipleStudentsAndTeachersAreSyncedCorrectly() {

        setupMoodleGetCourseResponse();

        setupOodiCourseUnitRealisationResponse("/oodi/course-realisation-multiple-students.json");

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()));

        expectFindStudentRequestToMoodle(STUDENT_NUMBER, STUDENT_USERNAME, STUDENT_USER_MOODLE_ID);
        expectFindStudentRequestToMoodle(STUDENT_NUMBER + "1", STUDENT_USERNAME + "1", STUDENT_USER_MOODLE_ID + 1);
        expectFindStudentRequestToMoodle(STUDENT_NUMBER + "2", STUDENT_USERNAME + "2", STUDENT_USER_MOODLE_ID + 2);
        expectFindStudentRequestToMoodle(STUDENT_NUMBER + "3", STUDENT_USERNAME + "3", STUDENT_USER_MOODLE_ID + 3);

        expectFindTeacherRequestToMoodle(TEACHER_ID_WITH_PREFIX, TEACHER_USERNAME, TEACHER_USER_MOODLE_ID);
        expectFindTeacherRequestToMoodle(TEACHER_ID_WITH_PREFIX + "1", TEACHER_USERNAME + "1", TEACHER_USER_MOODLE_ID + 1);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID + 1, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID + 1, MOODLE_COURSE_ID),

            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID + 2, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID + 2, MOODLE_COURSE_ID),

            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID + 3, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID + 3, MOODLE_COURSE_ID),

            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),

            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID + 1, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID + 1, MOODLE_COURSE_ID));

        expectAssignRolesToMoodle(false,
            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    @Test
    public void thatSynchronizationSummaryContainsErrorStatusesAndMessagesWhenMoodleResponseWithError() {
        testSynchronizationSummary(SynchronizationType.FULL, ERROR_RESPONSE, true);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponseWithEmptyString() {
        testSynchronizationSummary(SynchronizationType.FULL, EMPTY_OK_RESPONSE, false);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponseWithNull() {
        testSynchronizationSummary(SynchronizationType.FULL, NULL_OK_RESPONSE, false);
    }

    @Test
    public void thatIfApprovedIsMissingOnStudentItIsConsideredTrue() {

        setupMoodleGetCourseResponse();

        setupOodiCourseUnitRealisationResponse("/oodi/course-realisation-missing-approved.json");

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()));

        expectFindStudentRequestToMoodle(STUDENT_NUMBER, STUDENT_USERNAME, STUDENT_USER_MOODLE_ID);
        expectFindStudentRequestToMoodle(STUDENT_NUMBER + "1", STUDENT_USERNAME + "1", STUDENT_USER_MOODLE_ID + 1);
        expectFindStudentRequestToMoodle(STUDENT_NUMBER + "2", STUDENT_USERNAME + "2", STUDENT_USER_MOODLE_ID + 2);
        expectFindStudentRequestToMoodle(STUDENT_NUMBER + "3", STUDENT_USERNAME + "3", STUDENT_USER_MOODLE_ID + 3);

        expectFindTeacherRequestToMoodle(TEACHER_ID_WITH_PREFIX, TEACHER_USERNAME, TEACHER_USER_MOODLE_ID);
        expectFindTeacherRequestToMoodle(TEACHER_ID_WITH_PREFIX + "1", TEACHER_USERNAME + "1", TEACHER_USER_MOODLE_ID + 1);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID + 1, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID + 1, MOODLE_COURSE_ID),

            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID + 2, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID + 2, MOODLE_COURSE_ID),

            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID + 3, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID + 3, MOODLE_COURSE_ID),

            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),

            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID + 1, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID + 1, MOODLE_COURSE_ID));

        job.execute();
    }
}