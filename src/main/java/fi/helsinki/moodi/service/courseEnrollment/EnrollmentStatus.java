package fi.helsinki.moodi.service.courseEnrollment;

import fi.helsinki.moodi.service.importing.EnrollmentWarning;
import fi.helsinki.moodi.service.synchronize.process.EnrollmentSynchronizationStatus;

public class EnrollmentStatus {
    protected CourseEnrollmentStatusCode convertStatusCode(EnrollmentSynchronizationStatus enrollmentSynchronizationStatus) {
        switch(enrollmentSynchronizationStatus) {
            case COMPLETED:
                return CourseEnrollmentStatusCode.OK;
            case MOODLE_USER_NOT_FOUND:
                return CourseEnrollmentStatusCode.FAILED_NO_MOODLE_USER;
            case USERNAME_NOT_FOUND:
                return CourseEnrollmentStatusCode.FAILED_NO_USERNAME;
            default:
                return CourseEnrollmentStatusCode.FAILED;
        }
    }

    protected CourseEnrollmentStatusCode convertStatusCode(EnrollmentWarning enrollmentWarning) {
        switch(enrollmentWarning.code) {
            case EnrollmentWarning.CODE_USER_NOT_FOUND_FROM_ESB:
                return CourseEnrollmentStatusCode.FAILED_NO_USERNAME;
            case EnrollmentWarning.CODE_USER_NOT_FOUND_FROM_MOODLE:
                return CourseEnrollmentStatusCode.FAILED_NO_MOODLE_USER;
            default:
                return CourseEnrollmentStatusCode.FAILED;
        }
    }
}
