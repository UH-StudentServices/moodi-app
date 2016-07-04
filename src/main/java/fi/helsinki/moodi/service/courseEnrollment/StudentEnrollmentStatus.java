package fi.helsinki.moodi.service.courseEnrollment;

import fi.helsinki.moodi.service.importing.Enrollment;
import fi.helsinki.moodi.service.importing.EnrollmentWarning;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;

public class StudentEnrollmentStatus extends EnrollmentStatus {
    private final CourseEnrollmentStatusCode courseEnrollmentStatusCode;
    private final String studentNumber;

    public StudentEnrollmentStatus(StudentSynchronizationItem studentSynchronizationItem) {
        this.courseEnrollmentStatusCode = convertStatusCode(studentSynchronizationItem.getEnrollmentSynchronizationStatus());
        this.studentNumber = studentSynchronizationItem.getStudent().studentNumber;
    }

    public StudentEnrollmentStatus(Enrollment successfullEnrollment) {
        this.courseEnrollmentStatusCode = CourseEnrollmentStatusCode.OK;
        this.studentNumber = successfullEnrollment.studentNumber.get();
    }

    public StudentEnrollmentStatus(EnrollmentWarning failedEnrollment) {
        this.courseEnrollmentStatusCode = convertStatusCode(failedEnrollment);
        this.studentNumber = failedEnrollment.enrollment.studentNumber.get();
    }

    public CourseEnrollmentStatusCode getCourseEnrollmentStatusCode() {
        return courseEnrollmentStatusCode;
    }

    public String getStudentNumber() {
        return studentNumber;
    }
}
