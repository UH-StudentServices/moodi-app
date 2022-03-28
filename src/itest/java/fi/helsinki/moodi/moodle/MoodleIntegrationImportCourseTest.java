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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class MoodleIntegrationImportCourseTest extends AbstractMoodleIntegrationTest {
    @Test
    public void testMoodleIntegrationWhenImportingCourse() {
        String sisuCourseId = getSisuCourseId();

        expectCreator(creatorUser);

        expectCourseRealisationsWithUsers(
            sisuCourseId,
            newArrayList(studentUser, studentUserNotInMoodle),
            newArrayList(teacherUser)
            );

        long moodleCourseId = importCourse(sisuCourseId, creatorUser.personId);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);
        assertEquals(3, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(CREATOR_USERNAME, moodleUserEnrollmentsList);

        List<MoodleFullCourse> moodleCourses = moodleClient.getCourses(Arrays.asList(moodleCourseId));

        assertThat(moodleCourses.size()).isEqualTo(1);
        MoodleFullCourse mfc = moodleCourses.get(0);

        assertThat(mfc.fullName).isEqualTo("Lapsuus ja yhteiskunta");
        assertThat(mfc.displayName).isEqualTo("Lapsuus ja yhteiskunta");
        assertThat(mfc.endDate).isGreaterThan(mfc.startDate);
        // The unique shortname suffix for integration tests is derived from the current time and
        // will stay the same length until the year ~2055.
        assertThat(mfc.shortName).startsWith("Lapsuus ja yhtei-");
        assertThat(mfc.idNumber).isEqualTo(sisuCourseId);
        assertThat(mfc.lang).isEmpty();
        assertThat(mfc.summary).startsWith("<p>https://courses.helsinki.fi/fi/OODI-FLOW/136394381</p><p>Opintojaksot FOO123, BAR234</p><p>Kurssi");
        assertThat(mfc.categoryId).isEqualTo(9);
    }

}
