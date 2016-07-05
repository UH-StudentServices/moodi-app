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
