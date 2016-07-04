package fi.helsinki.moodi.service.importing;

public class EnrollmentWarning {

    public static final String CODE_USER_NOT_FOUND_FROM_ESB = "user-not-found-from-esb";
    public static final String CODE_USER_NOT_FOUND_FROM_MOODLE = "user-not-found-from-moodle";
    public static final String CODE_ENROLLMENT_FAILED = "enrollment-failed";

    public final String code;
    public final Enrollment enrollment;

    public EnrollmentWarning(
            String code,
            Enrollment enrollment) {

        this.code = code;
        this.enrollment = enrollment;
    }

    public static EnrollmentWarning userNotFoundFromEsb(final Enrollment enrollment) {
        return new EnrollmentWarning(CODE_USER_NOT_FOUND_FROM_ESB, enrollment);
    }

    public static EnrollmentWarning userNotFoundFromMoodle(final Enrollment enrollment) {
        return new EnrollmentWarning(CODE_USER_NOT_FOUND_FROM_MOODLE, enrollment);
    }

    public static EnrollmentWarning enrollFailed(final Enrollment enrollment) {
        return new EnrollmentWarning(CODE_ENROLLMENT_FAILED, enrollment);
    }
}