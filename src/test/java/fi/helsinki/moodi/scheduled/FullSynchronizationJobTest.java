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
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static fi.helsinki.moodi.util.DateFormat.OODI_UTC_DATE_FORMAT;
import static org.junit.Assert.*;
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

    @Autowired
    private FullSynchronizationJob job;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseEnrollmentStatusService courseEnrollmentStatusService;

    @Autowired
    private MapperService mapperService;

    private void setUpMockServerResponses(String endDate, boolean approved) {

        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-courses-12345.json"), MediaType.APPLICATION_JSON));

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

    private void setUpPositiveMockServerResponses() {
        expectGetEnrollmentsRequestToMoodle(MOODLE_COURSE_ID);

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));
    }


    @Test
    public void thatCourseIsSynchronized() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, true);
        setUpPositiveMockServerResponses();

        job.execute();
    }

    @Test
    public void thatOverYearOldCourseIsRemoved() {
        String endDateInPast = LocalDateTime.now().minusDays(1).minusYears(1).format(DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT));
        setUpMockServerResponses(endDateInPast, true);

        Course course = findCourse();

        assertFalse(course.removed);

        job.execute();

        course = findCourse();

        assertTrue(course.removed);
        assertEquals(course.removedMessage, EnrichmentStatus.OODI_COURSE_ENDED.toString());
    }

    @Test
    public void thatEndedCourseIsStillSynched() {
        String endDateInPast = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT));
        setUpMockServerResponses(endDateInPast, true);
        setUpPositiveMockServerResponses();

        Course course = findCourse();

        assertFalse(course.removed);

        job.execute();

        course = findCourse();

        assertFalse(course.removed);
    }

    @Test
    public void thatImportStatusIsSetToCompletedAndEnrollmentStatusesAreGenerated() {
        assertImportStatus(ImportStatus.COMPLETED_FAILED);

        assertNull(courseEnrollmentStatusService.getCourseEnrollmentStatus(REALISATION_ID));

        thatCourseIsSynchronized();

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

        expectAssignRolesToMoodle(false, new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

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
        expectFindStudentRequestToEsb(STUDENT_NUMBER, STUDENT_USERNAME);
        expectGetUserRequestToMoodle(STUDENT_USERNAME + USERNAME_SUFFIX, STUDENT_USER_MOODLE_ID);

        expectFindEmployeeRequestToEsb(TEACHER_ID_WITH_PREFIX, TEACHER_USERNAME);
        expectGetUserRequestToMoodle(TEACHER_USERNAME + USERNAME_SUFFIX, TEACHER_USER_MOODLE_ID);
    }

    private Course findCourse() {
        return courseService.findByRealisationId(REALISATION_ID).get();
    }

    private void assertImportStatus(ImportStatus expectedImportStatus) {
        Course course = findCourse();

        assertEquals(course.importStatus, expectedImportStatus);
    }

}