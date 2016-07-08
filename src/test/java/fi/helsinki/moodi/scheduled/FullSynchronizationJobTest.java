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
import fi.helsinki.moodi.test.fixtures.Fixtures;
import fi.helsinki.moodi.web.AbstractCourseControllerTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;
import static fi.helsinki.moodi.util.DateFormat.OODI_UTC_DATE_FORMAT;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class FullSynchronizationJobTest extends AbstractCourseControllerTest {

    private static final long REALISATION_ID = 12345;
    private static final int MOODLE_COURSE_ID = 54321;
    private static final int STUDENT_USER_MOODLE_ID = 555;
    private static final int TEACHER_USER_MOODLE_ID = 3434;
    private static final String STUDENT_NUMBER = "010342729";
    private static final String TEACHER_ID = "110588";
    private static final String TEACHER_ID_WITH_PREFIX = 9 + TEACHER_ID;

    @Autowired
    private FullSynchronizationJob job;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseEnrollmentStatusService courseEnrollmentStatusService;

    private void setUpMockServerResponses(String endDate) {

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
                        .build()),
                MediaType.APPLICATION_JSON));
    }

    @Test
    public void thatCourseIsSynchronized() {
        String endDateInFuture = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT));
        setUpMockServerResponses(endDateInFuture);

        expectGetEnrollmentsRequestToMoodle(MOODLE_COURSE_ID);

        expectFindStudentRequestToEsb(STUDENT_NUMBER, "niina");
        expectGetUserRequestToMoodle("niina@helsinki.fi", "555");

        expectFindEmployeeRequestToEsb(TEACHER_ID_WITH_PREFIX, "jukkapalmu");
        expectGetUserRequestToMoodle("jukkapalmu@helsinki.fi", "3434");

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        job.execute();
    }

    @Test
    @Ignore
    public void thatEndedCourseIsRemoved() {
        String endDateInPast = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT));
        setUpMockServerResponses(endDateInPast);

        Course course = findCourse();

        assertFalse(course.removed);

        job.execute();

        course = findCourse();

        assertTrue(course.removed);
        assertEquals(course.removedMessage, EnrichmentStatus.OODI_COURSE_ENDED.toString());
    }

    @Test
    public void thatImportStatusIsSetToCompletedAndEnrollmentStatusesAreGenerated() {
        assertImportStatus(ImportStatus.COMPLETED_FAILED);

        assertNull(courseEnrollmentStatusService.getCourseEnrollmentStatus(REALISATION_ID));

        thatCourseIsSynchronized();

        assertNotNull(courseEnrollmentStatusService.getCourseEnrollmentStatus(REALISATION_ID));

        assertImportStatus(ImportStatus.COMPLETED);
    }

    private Course findCourse() {
        return courseService.findByRealisationId(REALISATION_ID).get();
    }

    private void assertImportStatus(ImportStatus expectedImportStatus) {
        Course course = findCourse();

        assertEquals(course.importStatus, expectedImportStatus);
    }

}