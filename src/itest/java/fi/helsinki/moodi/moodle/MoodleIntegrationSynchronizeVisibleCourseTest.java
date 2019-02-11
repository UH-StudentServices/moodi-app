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

import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

/**
 * If you need your test courses to be visible in Moodle, use this class.
 */
@TestPropertySource(properties = "test.MoodleCourseBuilder.courseVisibility=true")
public class MoodleIntegrationSynchronizeVisibleCourseTest extends AbstractMoodleIntegrationTest {
    @Test
    public void testSyncRemovesStudentRoleAndSuspendsIfNotApproved() {
        long oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, singletonList(studentUser.setApproved(false)), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertUserCourseVisibility(true, STUDENT_USERNAME, moodleCourseId, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);

        synchronizationService.synchronize(SynchronizationType.FULL);

        moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertMoodiRoleEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertUserCourseVisibility(false, STUDENT_USERNAME, moodleCourseId, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }
}
