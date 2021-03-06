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

import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class MoodleIntegrationImportCourseTest extends AbstractMoodleIntegrationTest {
    @Test
    public void testMoodleIntegrationWhenImportingCourse() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationWithUsers(
            sisuCourseId,
            newArrayList(studentUser, studentUserNotInMoodle),
            singletonList(teacherUser));

        long moodleCourseId = importCourse(sisuCourseId);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        MoodleUserEnrollments studentEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, STUDENT_USERNAME);

        assertEquals(2, studentEnrollment.roles.size());
        assertTrue(studentEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(studentEnrollment.hasRole(mapperService.getStudentRoleId()));

        MoodleUserEnrollments teacherEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, TEACHER_USERNAME);

        assertEquals(2, teacherEnrollment.roles.size());
        assertTrue(teacherEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getTeacherRoleId()));

        List<MoodleFullCourse> moodleCourses = moodleClient.getCourses(Arrays.asList(moodleCourseId));

        assertThat(moodleCourses.size()).isEqualTo(1);
        MoodleFullCourse mfc = moodleCourses.get(0);

        assertThat(mfc.fullName).isEqualTo("Lapsuus ja yhteiskunta");
        assertThat(mfc.displayName).isEqualTo("Lapsuus ja yhteiskunta");
        assertThat(mfc.endDate).isGreaterThan(mfc.startDate);
        assertThat(mfc.shortName).contains(sisuCourseId);
        assertThat(mfc.idNumber).isEqualTo("sisu_" + sisuCourseId);
        assertThat(mfc.lang).isEmpty();
        assertThat(mfc.summary).isEqualTo("https://courses.helsinki.fi/fi/OODI-FLOW/136394381");
    }

}
