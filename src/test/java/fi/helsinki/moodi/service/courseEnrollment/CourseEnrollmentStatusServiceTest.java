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

package fi.helsinki.moodi.service.courseEnrollment;

import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.importing.Enrollment;
import fi.helsinki.moodi.service.importing.EnrollmentWarning;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.process.EnrollmentSynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class CourseEnrollmentStatusServiceTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private CourseEnrollmentStatusService courseEnrollmentStatusService;

    @Autowired
    private CourseEnrollmentStatusRepository courseEnrollmentStatusRepository;

    @Autowired
    private CourseService courseService;

    private Course createCourse() {
        return courseService.createCourse(1L, 1L);
    }

    private StudentSynchronizationItem createStudentSynchronizationItem(String studentNumber, boolean success, EnrollmentSynchronizationStatus enrollmentSynchronizationStatus) {
        OodiStudent oodiStudent = new OodiStudent();
        oodiStudent.studentNumber = studentNumber;

        StudentSynchronizationItem studentSynchronizationItem = new StudentSynchronizationItem(oodiStudent, 1L, 1L);
        StudentSynchronizationItem completedStudentSynchronization = studentSynchronizationItem.setCompleted(success, "message", enrollmentSynchronizationStatus);

        return completedStudentSynchronization;
    }

    private TeacherSynchronizationItem createTeacherSynchronizationItem(String teacherId, boolean success, EnrollmentSynchronizationStatus enrollmentSynchronizationStatus) {
        OodiTeacher oodiTeacher = new OodiTeacher();
        oodiTeacher.teacherId = teacherId;

        TeacherSynchronizationItem teacherSynchronizationItem = new TeacherSynchronizationItem(oodiTeacher, 1L, 1L);
        TeacherSynchronizationItem completedTeacherSynchronizationItem = teacherSynchronizationItem.setCompleted(success, "message", enrollmentSynchronizationStatus);

        return completedTeacherSynchronizationItem;
    }

    private SynchronizationItem createSynchronizationItem(List<StudentSynchronizationItem> studentSynchronizationItems, List<TeacherSynchronizationItem> teacherSynchronizationItems) {
        Course course = createCourse();

        SynchronizationItem synchronizationItem = new SynchronizationItem(course, SynchronizationType.FULL);

        SynchronizationItem synchronizationItemWithStudentItems = synchronizationItem.setStudentItems(Optional.of(studentSynchronizationItems));

        SynchronizationItem synchronizationItemWithTeacherItems = synchronizationItemWithStudentItems.setTeacherItems(Optional.of(teacherSynchronizationItems));

        return synchronizationItemWithTeacherItems;
    }

    @Test
    public void thatItPersistsStatusCorrectlyWhenImporting() {

        Enrollment studentEnrollment = Enrollment.forStudent("12345");
        Enrollment teacherEnrollment = Enrollment.forTeacher("54321");

        List<Enrollment> enrollments = Lists.newArrayList(studentEnrollment, teacherEnrollment);

        List<EnrollmentWarning> enrollmentWarnings = Lists.newArrayList();

        courseEnrollmentStatusService.persistCourseEnrollmentStatus(1L, 1L, enrollments, enrollmentWarnings);

        CourseEnrollmentStatus courseEnrollmentStatus = courseEnrollmentStatusRepository.findTop1ByRealisationIdOrderByCreatedDesc(1L).get();

        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"studentNumber\":\"12345\"}]", courseEnrollmentStatus.studentEnrollments);
        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"teacherId\":\"54321\"}]", courseEnrollmentStatus.teacherEnrollments);
        assertEquals(1L, courseEnrollmentStatus.realisationId);
    }

    @Test
    public void thatItPersistsStatusCorrectlyWhenSynchronizing() {
        List<StudentSynchronizationItem> studentSynchronizationItems = Lists.newArrayList(createStudentSynchronizationItem("12345", true, EnrollmentSynchronizationStatus.COMPLETED));
        List<TeacherSynchronizationItem> teacherSynchronizationItems = Lists.newArrayList(createTeacherSynchronizationItem("54321", true, EnrollmentSynchronizationStatus.COMPLETED));

        SynchronizationItem synchronizationItem = createSynchronizationItem(studentSynchronizationItems, teacherSynchronizationItems);

        courseEnrollmentStatusService.persistCourseEnrollmentStatus(synchronizationItem);

        CourseEnrollmentStatus courseEnrollmentStatus = courseEnrollmentStatusRepository.findTop1ByRealisationIdOrderByCreatedDesc(1L).get();
        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"studentNumber\":\"12345\"}]", courseEnrollmentStatus.studentEnrollments);
        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"teacherId\":\"54321\"}]", courseEnrollmentStatus.teacherEnrollments);
        assertEquals(1L, courseEnrollmentStatus.realisationId);
    }

    @Test
    public void thatItPersistsStatusCorrectlyWhenThereAreSomeFailingEnrollmentsWhenImporting() {

        Enrollment studentEnrollment1 = Enrollment.forStudent("12345");
        Enrollment studentEnrollment2 = Enrollment.forStudent("123456");
        Enrollment teacherEnrollment1 = Enrollment.forTeacher("54321");
        Enrollment teacherEnrollment2 = Enrollment.forTeacher("543210");

        EnrollmentWarning studentEnrollmentWarning = EnrollmentWarning.userNotFoundFromEsb(studentEnrollment1);
        EnrollmentWarning teacherEnrollmentWarning = EnrollmentWarning.userNotFoundFromEsb(teacherEnrollment1);

        List<Enrollment> enrollments = Lists.newArrayList(studentEnrollment1, studentEnrollment2, teacherEnrollment1, teacherEnrollment2);

        List<EnrollmentWarning> enrollmentWarnings = Lists.newArrayList(studentEnrollmentWarning, teacherEnrollmentWarning);

        courseEnrollmentStatusService.persistCourseEnrollmentStatus(1L, 1L, enrollments, enrollmentWarnings);

        CourseEnrollmentStatus courseEnrollmentStatus = courseEnrollmentStatusRepository.findTop1ByRealisationIdOrderByCreatedDesc(1L).get();

        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"studentNumber\":\"123456\"},{\"courseEnrollmentStatusCode\":\"FAILED_NO_USERNAME\",\"studentNumber\":\"12345\"}]", courseEnrollmentStatus.studentEnrollments);
        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"teacherId\":\"543210\"},{\"courseEnrollmentStatusCode\":\"FAILED_NO_USERNAME\",\"teacherId\":\"54321\"}]", courseEnrollmentStatus.teacherEnrollments);

        assertEquals(1L, courseEnrollmentStatus.realisationId);
    }

    @Test
    public void thatItPersistsStatusCorrectlyWhenThereAreSomeFailingEnrollmentsWhenSynchronizing() {
        List<StudentSynchronizationItem> studentSynchronizationItems = Lists.newArrayList(
            createStudentSynchronizationItem("12345", true, EnrollmentSynchronizationStatus.COMPLETED),
            createStudentSynchronizationItem("123456", false, EnrollmentSynchronizationStatus.USERNAME_NOT_FOUND)
        );

        List<TeacherSynchronizationItem> teacherSynchronizationItems = Lists.newArrayList(
            createTeacherSynchronizationItem("54321", true, EnrollmentSynchronizationStatus.COMPLETED),
            createTeacherSynchronizationItem("543210", false, EnrollmentSynchronizationStatus.USERNAME_NOT_FOUND)
        );

        SynchronizationItem synchronizationItem = createSynchronizationItem(studentSynchronizationItems, teacherSynchronizationItems);

        courseEnrollmentStatusService.persistCourseEnrollmentStatus(synchronizationItem);

        CourseEnrollmentStatus courseEnrollmentStatus = courseEnrollmentStatusRepository.findTop1ByRealisationIdOrderByCreatedDesc(1L).get();

        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"studentNumber\":\"12345\"},{\"courseEnrollmentStatusCode\":\"FAILED_NO_USERNAME\",\"studentNumber\":\"123456\"}]", courseEnrollmentStatus.studentEnrollments);
        assertEquals("[{\"courseEnrollmentStatusCode\":\"OK\",\"teacherId\":\"54321\"},{\"courseEnrollmentStatusCode\":\"FAILED_NO_USERNAME\",\"teacherId\":\"543210\"}]", courseEnrollmentStatus.teacherEnrollments);

        assertEquals(1L, courseEnrollmentStatus.realisationId);
    }

    @Test
    public void thatItPersistsStatusCorrectlyWhenAllEnrollmentsFailWhenImporting() {

        Enrollment studentEnrollment1 = Enrollment.forStudent("12345");
        Enrollment studentEnrollment2 = Enrollment.forStudent("123456");
        Enrollment teacherEnrollment = Enrollment.forTeacher("54321");

        EnrollmentWarning enrollmentWarning1 = EnrollmentWarning.userNotFoundFromMoodle(studentEnrollment1);
        EnrollmentWarning enrollmentWarning2 = EnrollmentWarning.userNotFoundFromMoodle(studentEnrollment2);
        EnrollmentWarning enrollmentWarning3 = EnrollmentWarning.enrollFailed(teacherEnrollment);

        List<Enrollment> enrollments = Lists.newArrayList(studentEnrollment1, studentEnrollment2, teacherEnrollment);

        List<EnrollmentWarning> enrollmentWarnings = Lists.newArrayList(enrollmentWarning1, enrollmentWarning2, enrollmentWarning3);

        courseEnrollmentStatusService.persistCourseEnrollmentStatus(1L, 1L, enrollments, enrollmentWarnings);

        CourseEnrollmentStatus courseEnrollmentStatus = courseEnrollmentStatusRepository.findTop1ByRealisationIdOrderByCreatedDesc(1L).get();

        assertEquals("[{\"courseEnrollmentStatusCode\":\"FAILED_NO_MOODLE_USER\",\"studentNumber\":\"12345\"},{\"courseEnrollmentStatusCode\":\"FAILED_NO_MOODLE_USER\",\"studentNumber\":\"123456\"}]", courseEnrollmentStatus.studentEnrollments);
        assertEquals("[{\"courseEnrollmentStatusCode\":\"FAILED\",\"teacherId\":\"54321\"}]", courseEnrollmentStatus.teacherEnrollments);
        assertEquals(1L, courseEnrollmentStatus.realisationId);
    }

    @Test
    public void thatItPersistsStatusCorrectlyWhenAllEnrollmentsFailWhenSynchronizing() {
        List<StudentSynchronizationItem> studentSynchronizationItems = Lists.newArrayList(
            createStudentSynchronizationItem("12345", false, EnrollmentSynchronizationStatus.MOODLE_USER_NOT_FOUND),
            createStudentSynchronizationItem("123456", false, EnrollmentSynchronizationStatus.MOODLE_USER_NOT_FOUND)
        );

        List<TeacherSynchronizationItem> teacherSynchronizationItems = Lists.newArrayList(
            createTeacherSynchronizationItem("54321", false, EnrollmentSynchronizationStatus.ERROR)
        );

        SynchronizationItem synchronizationItem = createSynchronizationItem(studentSynchronizationItems, teacherSynchronizationItems);

        courseEnrollmentStatusService.persistCourseEnrollmentStatus(synchronizationItem);

        CourseEnrollmentStatus courseEnrollmentStatus = courseEnrollmentStatusRepository.findTop1ByRealisationIdOrderByCreatedDesc(1L).get();

        assertEquals("[{\"courseEnrollmentStatusCode\":\"FAILED_NO_MOODLE_USER\",\"studentNumber\":\"12345\"},{\"courseEnrollmentStatusCode\":\"FAILED_NO_MOODLE_USER\",\"studentNumber\":\"123456\"}]", courseEnrollmentStatus.studentEnrollments);
        assertEquals("[{\"courseEnrollmentStatusCode\":\"FAILED\",\"teacherId\":\"54321\"}]", courseEnrollmentStatus.teacherEnrollments);
        assertEquals(1L, courseEnrollmentStatus.realisationId);
    }


}
