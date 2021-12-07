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
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCreator(creatorUser);

        long moodleCourseId = importCourse(sisuCourseId, creatorUser.personId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), singletonList(teacherUser));
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(3, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(CREATOR_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewStudentEnrollment() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, new ArrayList<>(), singletonList(teacherUser));

        long moodleCourseId = importCourse(sisuCourseId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), new ArrayList<>());
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewTeacherEnrollment() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), new ArrayList<>());

        long moodleCourseId = importCourse(sisuCourseId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), singletonList(teacherUser));
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewStudentAndTeacherEnrollment() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, new ArrayList<>(), new ArrayList<>());

        long moodleCourseId = importCourse(sisuCourseId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), singletonList(teacherUser));
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncNewHybridUserEnrollment() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, new ArrayList<>(), new ArrayList<>());

        long moodleCourseId = importCourse(sisuCourseId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, singletonList(teacherInStudentRole), singletonList(teacherUser));
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(1, moodleUserEnrollmentsList.size());

        assertHybridEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncAddTeacherRoleToStudent() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), singletonList(teacherUser));

        long moodleCourseId = importCourse(sisuCourseId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), newArrayList(teacherUser, studentUserInTeacherRole));
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertHybridEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncAddStudentRoleToTeacher() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), singletonList(teacherUser));

        long moodleCourseId = importCourse(sisuCourseId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, newArrayList(studentUser, teacherInStudentRole), singletonList(teacherUser));
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertHybridEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncRemovesRolesIfSisuDoesNotReturnEnrolledUsers() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, singletonList(studentUser), singletonList(teacherUser));

        long moodleCourseId = importCourse(sisuCourseId);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, new ArrayList<>(), new ArrayList<>());
        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertMoodiRoleEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncRemovesStudentRoleFromHybridUserIfNotApproved() {
        String sisuCourseId = getSisuCourseId();

        expectCourseRealisationsWithUsers(sisuCourseId, singletonList(teacherInStudentRole), singletonList(teacherUser));

        long moodleCourseId = importCourse(sisuCourseId);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertHybridEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);

        resetAndExpectCourseRealisationsWithUsers(sisuCourseId, singletonList(teacherInStudentRole.setEnrolled(false)), singletonList(teacherUser));
        synchronizationService.synchronize(SynchronizationType.FULL);

        moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(1, moodleUserEnrollmentsList.size());

        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }
}
