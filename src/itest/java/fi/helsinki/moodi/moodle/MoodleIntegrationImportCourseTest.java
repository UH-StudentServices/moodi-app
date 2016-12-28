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
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportingService;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Random;

import static fi.helsinki.moodi.integration.esb.EsbService.DOMAIN_SUFFIX;
import static fi.helsinki.moodi.integration.esb.EsbService.TEACHER_ID_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleIntegrationImportCourseTest extends AbstractMoodleIntegrationTest {

    private static final String STUDENT_NUMBER = "014010293";
    private static final String STUDENT_USERNAME = "bar_simp";
    private static final String TEACHER_ID = "011631484";
    private static final String TEACHER_USERNAME = "mag_simp";

    @Autowired
    private ImportingService importingService;

    @Autowired
    private MoodleClient moodleClient;

    @Autowired
    private MapperService mapperService;

    private long getCourseId() {
        return new Random().nextInt();
    }

    @Test
    public void testMoodleIntegrationWhenImportingCourse() {
        long courseId = getCourseId();

        expectGetCourseUnitRealisationRequestToOodi(
            courseId,
            withSuccess(Fixtures.asString(
                "src/itest/resources/fixtures/",
                "/oodi/itest-course-realisation.json",
                new ImmutableMap.Builder()
                    .put("courseId", courseId)
                    .put("studentNumber", STUDENT_NUMBER)
                    .put("studentApproved", true)
                    .put("teacherId", TEACHER_ID)
                    .build()),
                MediaType.APPLICATION_JSON));

        expectFindStudentRequestToEsb(STUDENT_NUMBER, STUDENT_USERNAME);
        expectFindEmployeeRequestToEsb(TEACHER_ID_PREFIX + TEACHER_ID, TEACHER_USERNAME);

        long moodleCourseId = importingService.importCourse(new ImportCourseRequest(courseId)).data
            .map(c -> c.moodleCourseId).orElseThrow(() -> new RuntimeException("Course import failed"));

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        MoodleUserEnrollments studentEnrollment = findEnrollmentsByRole(moodleUserEnrollmentsList, mapperService.getStudentRoleId());
        MoodleUserEnrollments teacherEnrollment = findEnrollmentsByRole(moodleUserEnrollmentsList, mapperService.getTeacherRoleId());

        assertEquals(STUDENT_USERNAME + DOMAIN_SUFFIX, studentEnrollment.username);
        assertEquals(TEACHER_USERNAME + DOMAIN_SUFFIX, teacherEnrollment.username);

        assertEquals(2, studentEnrollment.roles.size());
        assertTrue(studentEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(studentEnrollment.hasRole(mapperService.getStudentRoleId()));

        assertEquals(2, teacherEnrollment.roles.size());
        assertTrue(teacherEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getTeacherRoleId()));

    }

    private MoodleUserEnrollments findEnrollmentsByRole(List<MoodleUserEnrollments> moodleUserEnrollmentsList, long roleId) {
        return moodleUserEnrollmentsList.stream()
            .filter(e -> e.hasRole(roleId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Teacher enrollment not found"));
    }


}
