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
import fi.helsinki.moodi.service.synchronize.process.EnrollmentSynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public abstract class AbstractSynchronizationJobTest extends AbstractMoodiIntegrationTest {

    protected static final long REALISATION_ID = 12345;
    protected static final int MOODLE_COURSE_ID = 54321;
    protected static final int STUDENT_USER_MOODLE_ID = 555;
    protected static final int TEACHER_USER_MOODLE_ID = 3434;
    protected static final String STUDENT_NUMBER = "010342729";
    protected static final String TEACHER_ID = "110588";
    protected static final String TEACHER_ID_WITH_PREFIX = 9 + TEACHER_ID;
    protected static final String STUDENT_USERNAME = "niina";
    protected static final String TEACHER_USERNAME = "jukkapalmu";
    protected static final String USERNAME_SUFFIX = "@helsinki.fi";

    private static final String SUMMARY_MESSAGE_ENROLLMENT_FAILED = "Enrolment failed";
    private static final String SUMMARY_MESSAGE_ENROLLMENT_SUCCEEDED = "Enrolment succeeded";
    private static final String SUMMARY_MESSAGE_ROLE_ADD_FAILED = "Role add failed";
    private static final String SUMMARY_MESSAGE_ROLE_ADD_SUCCEEDED = "Role add succeeded";

    @Autowired
    protected CourseService courseService;

    @Autowired
    protected FullSynchronizationJob job;

    @Autowired
    protected CourseEnrollmentStatusService courseEnrollmentStatusService;

    @Autowired
    protected MapperService mapperService;

    @Autowired
    protected SynchronizationService synchronizationService;

    protected void setUpMockServerResponses(String endDate, boolean approved) {
        setupMoodleGetCourseResponse();
        setupOodiCourseUnitRealisationResponse(endDate, approved);
    }

    protected void setupMoodleGetCourseResponse() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-courses-12345.json"), MediaType.APPLICATION_JSON));
    }

    protected void setupOodiCourseUnitRealisationResponse(String endDate, boolean approved) {
        expectGetCourseUnitRealisationRequestToOodi(
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

    protected void testSynchronizationSummary(SynchronizationType synchronizationType, String moodleResponse, boolean expectErrors) {
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

        SynchronizationSummary summary = synchronizationService.synchronize(synchronizationType);

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

    protected SynchronizationSummary testTresholdCheckFailed(String expectedMessage) {
        setUpMockServerResponses(getFutureDateString(), false);

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()));

        expectFindUsersRequestsToMoodle();

        SynchronizationSummary summary = synchronizationService.synchronize(SynchronizationType.FULL);

        SynchronizationItem item = summary.getItems().get(0);

        assertEquals(item.getProcessingStatus(), ProcessingStatus.LOCKED);

        assertEquals(item.getMessage(), expectedMessage);

        return summary;
    }

    protected void setupOodiCourseUnitRealisationResponse(String responseJson) {
        expectGetCourseUnitRealisationRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                    responseJson,
                    new ImmutableMap.Builder()
                        .put("endDate", getFutureDateString())
                        .build()),
                MediaType.APPLICATION_JSON));
    }

    protected String getEnrollmentsResponse(int moodleUserId, long moodleRoleId, long moodiRoleId) {
        return String.format(
            "[{ \"id\" : \"%s\" , \"roles\" : [{\"roleid\" : %s}, {\"roleid\" : %s}]}]",
            moodleUserId,
            moodleRoleId,
            moodiRoleId);
    }

    protected void expectFindUsersRequestsToMoodle() {
        expectFindStudentRequestToMoodle(STUDENT_NUMBER, STUDENT_USERNAME, STUDENT_USER_MOODLE_ID);
        expectFindTeacherRequestToMoodle(TEACHER_ID_WITH_PREFIX, TEACHER_USERNAME, TEACHER_USER_MOODLE_ID);
    }

    protected void expectFindStudentRequestToMoodle(String studentNumber, String username, int moodleId) {
        expectFindStudentRequestToEsb(studentNumber, username);
        expectGetUserRequestToMoodle(username + USERNAME_SUFFIX, moodleId);
    }

    protected void expectFindTeacherRequestToMoodle(String teacherId, String username, int moodleId) {
        expectFindEmployeeRequestToEsb(teacherId, username);
        expectGetUserRequestToMoodle(username + USERNAME_SUFFIX, moodleId);
    }


    protected Course findCourse() {
        return courseService.findByRealisationId(REALISATION_ID).get();
    }

    protected void assertImportStatus(Course.ImportStatus expectedImportStatus) {
        Course course = findCourse();

        assertEquals(course.importStatus, expectedImportStatus);
    }
}
