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

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class MoodleIntegrationSynchronizeCourseTest extends AbstractMoodleIntegrationTest {

    @Test
    public void testSyncExistingUsers() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewStudentEnrollment() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, new ArrayList<>(), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, singletonList(studentUser), new ArrayList<>());

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewTeacherEnrollment() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), new ArrayList<>());
        expectCourseUsersWithUsers(oodiCourseId, new ArrayList<>(), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewStudentAndTeacherEnrollment() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, new ArrayList<>(), new ArrayList<>());
        expectCourseUsersWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewHybridUserEnrollment() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, new ArrayList<>(), new ArrayList<>());
        expectCourseUsersWithUsers(oodiCourseId, singletonList(teacherInStudentRole), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(1, moodleUserEnrollmentsList.size());

        assertHybridEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncAddTeacherRoleToStudent() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, singletonList(studentUser), newArrayList(teacherUser, studentUserInTeacherRole));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertHybridEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncAddStudentRoleToTeacher() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, newArrayList(studentUser, teacherInStudentRole), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertHybridEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncDoesNotRemoveRolesIfOodiDoesNotReturnEnrolledUsers() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, new ArrayList<>(), new ArrayList<>());

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncRemovesStudentRoleFromHybridUserIfNotApproved() {
        String oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(teacherInStudentRole), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, singletonList(teacherInStudentRole.setApproved(false)), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertHybridEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);

        synchronizationService.synchronize(SynchronizationType.FULL);

        moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(1, moodleUserEnrollmentsList.size());

        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }
}
