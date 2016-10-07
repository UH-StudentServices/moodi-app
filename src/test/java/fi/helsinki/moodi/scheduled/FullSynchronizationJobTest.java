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

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatusService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.EnrollmentSynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import fi.helsinki.moodi.test.util.DateUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static fi.helsinki.moodi.test.util.DateUtil.getPastDateString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class FullSynchronizationJobTest extends AbstractMoodiIntegrationTest {

    private static final long REALISATION_ID = 12345;
    private static final int MOODLE_COURSE_ID = 54321;
    private static final int STUDENT_USER_MOODLE_ID = 555;
    private static final int TEACHER_USER_MOODLE_ID = 3434;
    private static final String STUDENT_NUMBER = "010342729";
    private static final String TEACHER_ID = "110588";
    private static final String TEACHER_ID_WITH_PREFIX = 9 + TEACHER_ID;
    private static final String STUDENT_USERNAME = "niina";
    private static final String TEACHER_USERNAME = "jukkapalmu";
    private static final String USERNAME_SUFFIX = "@helsinki.fi";

    private static final String SUMMARY_MESSAGE_ENROLLMENT_FAILED = "Enrolment failed";
    private static final String SUMMARY_MESSAGE_ENROLLMENT_SUCCEEDED = "Enrolment succeeded";
    private static final String SUMMARY_MESSAGE_ROLE_ADD_FAILED = "Role add failed";
    private static final String SUMMARY_MESSAGE_ROLE_ADD_SUCCEEDED = "Role add succeeded";

    @Autowired
    private FullSynchronizationJob job;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseEnrollmentStatusService courseEnrollmentStatusService;

    @Autowired
    private MapperService mapperService;

    @Autowired
    private SynchronizationService synchronizationService;

    private void setUpMockServerResponses(String endDate, boolean approved) {
        setupMoodleGetCourseResponse();
        setupOodiCourseUnitRealisationResponse(endDate, approved);
    }

    private void setupMoodleGetCourseResponse() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-courses-12345.json"), MediaType.APPLICATION_JSON));
    }

    private void setupOodiCourseUnitRealisationResponse(String endDate, boolean approved) {
        expectGetCourseRealisationUnitRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                    "/oodi/parameterized-course-realisation.json",
                    new ImmutableMap.Builder()
                        .put("studentnumber", STUDENT_NUMBER)
                        .put("teacherid", TEACHER_ID)
                        .put("endDate", endDate)
                        .put("deleted", false)
                        .put("approved", approved)
                        .build()),
                MediaType.APPLICATION_JSON));
    }

    private void setupOodiCourseUnitRealisationResponse(String responseJson) {
        expectGetCourseRealisationUnitRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                    responseJson,
                    new ImmutableMap.Builder()
                        .put("endDate", getFutureDateString())
                        .build()),
                MediaType.APPLICATION_JSON));
    }

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

    @Ignore
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

        expectAssignRolesToMoodle(false, new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    @Ignore
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

        expectAssignRolesToMoodle(false, new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    private void testSynchronizationSummary(String moodleResponse, boolean expectErrors) {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, true);

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()));

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodleWithResponse(moodleResponse,
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        expectAssignRolesToMoodleWithResponse(moodleResponse, true, new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

        SynchronizationSummary summary = synchronizationService.synchronize(SynchronizationType.FULL);

        SynchronizationItem item =  summary.getItems().get(0);
        TeacherSynchronizationItem teacherSynchronizationItem = item.getTeacherItems().get().get(0);
        StudentSynchronizationItem studentSynchronizationItem = item.getStudentItems().get().get(0);

        EnrollmentSynchronizationStatus expectedStatus = expectErrors ? EnrollmentSynchronizationStatus.ERROR : EnrollmentSynchronizationStatus.COMPLETED;

        assertEquals(teacherSynchronizationItem.getEnrollmentSynchronizationStatus(), expectedStatus);
        assertEquals(studentSynchronizationItem.getEnrollmentSynchronizationStatus(), expectedStatus);

        assertEquals(expectErrors ?
            SUMMARY_MESSAGE_ENROLLMENT_FAILED : SUMMARY_MESSAGE_ENROLLMENT_SUCCEEDED, teacherSynchronizationItem.getMessage());
        assertEquals(expectErrors ?
            SUMMARY_MESSAGE_ROLE_ADD_FAILED : SUMMARY_MESSAGE_ROLE_ADD_SUCCEEDED, studentSynchronizationItem.getMessage());
    }

    @Test
    public void thatSynchronizationSummaryContainsErrorStatusesAndMessagesWhenMoodleResponsWithError() {
        testSynchronizationSummary(ERROR_RESPONSE, true);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponsWithEmptyString() {
        testSynchronizationSummary(EMPTY_OK_RESPONSE, false);
    }

    @Test
    public void thatSynchronizationSummaryContainsOkStatusesAndMessagesWhenMoodleResponsWithNull() {
        testSynchronizationSummary(NULL_OK_RESPONSE, false);
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


    private String getEnrollmentsResponse(int moodleUserId, long moodleRoleId, long moodiRoleId) {
        return String.format(
            "[{ \"id\" : \"%s\" , \"roles\" : [{\"roleid\" : %s}, {\"roleid\" : %s}]}]",
            moodleUserId,
            moodleRoleId,
            moodiRoleId);
    }

    private void expectFindUsersRequestsToMoodle() {
        expectFindStudentRequestToMoodle(STUDENT_NUMBER, STUDENT_USERNAME, STUDENT_USER_MOODLE_ID);
        expectFindTeacherRequestToMoodle(TEACHER_ID_WITH_PREFIX, TEACHER_USERNAME, TEACHER_USER_MOODLE_ID);
    }

    private void expectFindStudentRequestToMoodle(String studentNumber, String username, int moodleId) {
        expectFindStudentRequestToEsb(studentNumber, username);
        expectGetUserRequestToMoodle(username + USERNAME_SUFFIX, moodleId);
    }

    private void expectFindTeacherRequestToMoodle(String teacherId, String username, int moodleId) {
        expectFindEmployeeRequestToEsb(teacherId, username);
        expectGetUserRequestToMoodle(username + USERNAME_SUFFIX, moodleId);
    }


    private Course findCourse() {
        return courseService.findByRealisationId(REALISATION_ID).get();
    }

    private void assertImportStatus(ImportStatus expectedImportStatus) {
        Course course = findCourse();

        assertEquals(course.importStatus, expectedImportStatus);
    }

}