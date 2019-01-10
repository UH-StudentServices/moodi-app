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
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MoodleIntegrationSynchronizeCourseTest extends AbstractMoodleIntegrationTest {

    @Autowired
    private SynchronizationService synchronizationService;

    private void assertUserEnrollments(String username,
                                       List<MoodleUserEnrollments> moodleUserEnrollmentsList,
                                       List<Long> expectedRoleIds) {
        MoodleUserEnrollments userEnrollments = findEnrollmentsByUsername(moodleUserEnrollmentsList, username);

        assertEquals(expectedRoleIds.size(), userEnrollments.roles.size());

        expectedRoleIds.stream().forEach(roleId -> assertTrue(userEnrollments.hasRole(roleId)));
    }

    private void assertStudentEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            newArrayList(mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()));
    }

    private void assertTeacherEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            newArrayList(mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()));
    }

    private void assertHybridEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            newArrayList(mapperService.getStudentRoleId(), mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()));
    }
    /*
        Every user is that has been enrolled or updated by moodi is tagged with "moodi role".
        This role is never removed, even if other roles are removed. Note that only student role can be removed by Moodi
        if student is returned from Oodi with approved set to false.
     */

    private void assertMoodiRoleEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            singletonList(mapperService.getMoodiRoleId()));
    }

    @Test
    public void testSyncExistingUsers() {
        long oodiCourseId = getOodiCourseId();

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
        long oodiCourseId = getOodiCourseId();

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
        long oodiCourseId = getOodiCourseId();

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
        long oodiCourseId = getOodiCourseId();

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
        long oodiCourseId = getOodiCourseId();

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
        long oodiCourseId = getOodiCourseId();

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
        long oodiCourseId = getOodiCourseId();

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
        long oodiCourseId = getOodiCourseId();

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
    public void testSyncRemovesStudentRoleIfNotApproved() {
        long oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, singletonList(studentUser.setApproved(false)), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertStudentEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);

        synchronizationService.synchronize(SynchronizationType.FULL);

        moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        assertMoodiRoleEnrollment(STUDENT_USERNAME, moodleUserEnrollmentsList);
        assertTeacherEnrollment(TEACHER_USERNAME, moodleUserEnrollmentsList);
    }

    @Test
    public void testSyncRemovesStudentRoleFromHybridUserIfNotApproved() {
        long oodiCourseId = getOodiCourseId();

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
