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

package fi.helsinki.moodi.service.importing;

public class EnrollmentWarning {

    public static final String CODE_USER_NOT_FOUND_FROM_ESB = "user-not-found-from-esb";
    public static final String CODE_USER_NOT_FOUND_FROM_MOODLE = "user-not-found-from-moodle";
    public static final String CODE_ENROLLMENT_FAILED = "enrollment-failed";
    public static final String CODE_USER_NOT_APPROVED = "user-not-approved";

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

    public static EnrollmentWarning userNotApproved(final Enrollment enrollment) {
        return new EnrollmentWarning(CODE_USER_NOT_APPROVED, enrollment);
    }
}