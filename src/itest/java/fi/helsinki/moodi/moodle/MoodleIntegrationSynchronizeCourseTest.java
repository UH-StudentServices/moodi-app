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

package fi.helsinki.moodi.moodle;

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static fi.helsinki.moodi.integration.esb.EsbService.TEACHER_ID_PREFIX;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleIntegrationSynchronizeCourseTest extends AbstractMoodleIntegrationTest {

    @Autowired
    private SynchronizationService synchronizationService;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    public void testMoodleIntegrationWhenSyncingCourse() {
        courseRepository.deleteAll(); // Delete migrated courses for easier testing

        long oodiCourseId = getOodiCourseId();

        expectCourseImport(oodiCourseId);

        expectGetCourseUsers(oodiCourseId);

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        MoodleUserEnrollments studentEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, STUDENT_USERNAME);
        MoodleUserEnrollments teacherEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, TEACHER_USERNAME);

        assertEquals(2, studentEnrollment.roles.size());
        assertTrue(studentEnrollment.hasRole(mapperService.getStudentRoleId()));
        assertTrue(studentEnrollment.hasRole(mapperService.getMoodiRoleId()));

        assertEquals(3, teacherEnrollment.roles.size());
        assertTrue(teacherEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getStudentRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getTeacherRoleId()));
    }

    private void expectCourseImport(long courseId) {

        expectGetCourseUnitRealisationRequestToOodi(
            courseId,
            withSuccess(Fixtures.asString(
                INTEGRATION_TEST_OODI_FIXTURES_PREFIX,
                "course-realisation-sync-1.json",
                new ImmutableMap.Builder()
                    .put("courseId", courseId)
                    .put("teacherId", TEACHER_ID)
                    .put("endDate", getFutureDateString())
                    .build()),
                MediaType.APPLICATION_JSON));

        expectFindEmployeeRequestToEsb(TEACHER_ID_PREFIX + TEACHER_ID, TEACHER_USERNAME);
    }

    private void expectGetCourseUsers(long courseId) {
        expectGetCourseUsersRequestToOodi(
            courseId,
            withSuccess(Fixtures.asString(
                INTEGRATION_TEST_OODI_FIXTURES_PREFIX,
                "course-realisation-sync-2.json",
                new ImmutableMap.Builder()
                    .put("courseId", courseId)
                    .put("studentNumber", STUDENT_NUMBER)
                    .put("studentApproved", true)
                    .put("studentNumber2", STUDENT_NUMBER_2)
                    .put("studentApproved2", true)
                    .put("teacherId", TEACHER_ID)
                    .put("endDate", getFutureDateString())
                    .build()),
                MediaType.APPLICATION_JSON));

        expectFindStudentRequestToEsb(STUDENT_NUMBER, STUDENT_USERNAME);
        expectFindStudentRequestToEsb(STUDENT_NUMBER_2, STUDENT_USERNAME_2);
        expectFindEmployeeRequestToEsb(TEACHER_ID_PREFIX + TEACHER_ID, TEACHER_USERNAME);
    }


}
