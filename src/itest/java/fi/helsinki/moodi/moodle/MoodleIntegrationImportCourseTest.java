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
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static fi.helsinki.moodi.integration.esb.EsbService.TEACHER_ID_PREFIX;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleIntegrationImportCourseTest extends AbstractMoodleIntegrationTest {

    @Test
    public void testMoodleIntegrationWhenImportingCourse() {
        long oodiCourseId = getOodiCourseId();

        expectCourseImport(oodiCourseId);

        long moodleCourseId = importCourse(oodiCourseId);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        MoodleUserEnrollments studentEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, STUDENT_USERNAME);
        MoodleUserEnrollments teacherEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, TEACHER_USERNAME);

        assertEquals(2, studentEnrollment.roles.size());
        assertTrue(studentEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(studentEnrollment.hasRole(mapperService.getStudentRoleId()));

        assertEquals(2, teacherEnrollment.roles.size());
        assertTrue(teacherEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getTeacherRoleId()));

    }

    private void expectCourseImport(long courseId) {

        expectGetCourseUnitRealisationRequestToOodi(
            courseId,
            withSuccess(Fixtures.asString(
                INTEGRATION_TEST_OODI_FIXTURES_PREFIX,
                "course-realisation-import.json",
                new ImmutableMap.Builder()
                    .put("courseId", courseId)
                    .put("studentNumber", STUDENT_NUMBER)
                    .put("studentApproved", true)
                    .put("teacherId", TEACHER_ID)
                    .put("endDate", getFutureDateString())
                    .build()),
                MediaType.APPLICATION_JSON));

        expectFindStudentRequestToEsb(STUDENT_NUMBER, STUDENT_USERNAME);
        expectFindEmployeeRequestToEsb(TEACHER_ID_PREFIX + TEACHER_ID, TEACHER_USERNAME);
    }

}