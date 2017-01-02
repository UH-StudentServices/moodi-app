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
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MoodleIntegrationSynchronizeCourseTest extends AbstractMoodleIntegrationTest {

    @Autowired
    private SynchronizationService synchronizationService;

    @Autowired
    private CourseRepository courseRepository;

    @Before
    public void emptyCourses() {
        courseRepository.deleteAll(); // Delete migrated courses for easier testing
    }

    @Test
    public void testSyncNewEnrollments() {
        long oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, newArrayList(), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, newArrayList(studentUser), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        MoodleUserEnrollments studentEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, STUDENT_USERNAME);
        MoodleUserEnrollments teacherEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, TEACHER_USERNAME);

        assertEquals(2, studentEnrollment.roles.size());
        assertTrue(studentEnrollment.hasRole(mapperService.getStudentRoleId()));
        assertTrue(studentEnrollment.hasRole(mapperService.getMoodiRoleId()));

        assertEquals(2, teacherEnrollment.roles.size());
        assertTrue(teacherEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getTeacherRoleId()));
    }

    @Test
    public void testSyncRoleAdditions() {
        long oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, newArrayList(), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, newArrayList(teacherInStudentRole), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(1, moodleUserEnrollmentsList.size());

        MoodleUserEnrollments teacherEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, TEACHER_USERNAME);

        assertEquals(3, teacherEnrollment.roles.size());
        assertTrue(teacherEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getStudentRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getTeacherRoleId()));
    }

    @Test
    public void testSyncRoleRemoves() {
        long oodiCourseId = getOodiCourseId();

        expectCourseRealisationWithUsers(oodiCourseId, singletonList(studentUser), singletonList(teacherUser));
        expectCourseUsersWithUsers(oodiCourseId, singletonList(studentUser.setApproved(false)), singletonList(teacherUser));

        long moodleCourseId = importCourse(oodiCourseId);

        synchronizationService.synchronize(SynchronizationType.FULL);

        List<MoodleUserEnrollments> moodleUserEnrollmentsList = moodleClient.getEnrolledUsers(moodleCourseId);

        assertEquals(2, moodleUserEnrollmentsList.size());

        MoodleUserEnrollments studentEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, STUDENT_USERNAME);
        MoodleUserEnrollments teacherEnrollment = findEnrollmentsByUsername(moodleUserEnrollmentsList, TEACHER_USERNAME);

        assertEquals(1, studentEnrollment.roles.size());
        assertTrue(studentEnrollment.hasRole(mapperService.getMoodiRoleId()));

        assertEquals(2, teacherEnrollment.roles.size());
        assertTrue(teacherEnrollment.hasRole(mapperService.getMoodiRoleId()));
        assertTrue(teacherEnrollment.hasRole(mapperService.getTeacherRoleId()));
    }
}
