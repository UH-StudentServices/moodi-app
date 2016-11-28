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
            case EnrollmentWarning.CODE_USER_NOT_APPROVED:
                return CourseEnrollmentStatusCode.FAILED_NOT_APPROVED;
            default:
                return CourseEnrollmentStatusCode.FAILED;
        }
    }
}
