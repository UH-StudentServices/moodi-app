package fi.helsinki.moodi.service.courseEnrollment;

import fi.helsinki.moodi.service.importing.Enrollment;
import fi.helsinki.moodi.service.importing.EnrollmentWarning;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;

public class TeacherEnrollmentStatus extends EnrollmentStatus {
    private final CourseEnrollmentStatusCode courseEnrollmentStatusCode;
    private final String teacherId;

    public TeacherEnrollmentStatus(TeacherSynchronizationItem teacherSynchronizationItem) {
        this.courseEnrollmentStatusCode = convertStatusCode(teacherSynchronizationItem.getEnrollmentSynchronizationStatus());
        this.teacherId = teacherSynchronizationItem.getTeacher().teacherId;
    }

    public TeacherEnrollmentStatus(Enrollment successfullEnrollment) {
        this.courseEnrollmentStatusCode = CourseEnrollmentStatusCode.OK;
        this.teacherId = successfullEnrollment.teacherId.get();
    }

    public TeacherEnrollmentStatus(EnrollmentWarning failedEnrollment) {
        this.courseEnrollmentStatusCode = convertStatusCode(failedEnrollment);
        this.teacherId = failedEnrollment.enrollment.teacherId.get();
    }

    public TeacherEnrollmentStatus(CourseEnrollmentStatusCode courseEnrollmentStatusCode, String teacherId) {
        this.courseEnrollmentStatusCode = courseEnrollmentStatusCode;
        this.teacherId = teacherId;
    }


    public CourseEnrollmentStatusCode getCourseEnrollmentStatusCode() {
        return courseEnrollmentStatusCode;
    }

    public String getTeacherId() {
        return teacherId;
    }
}
