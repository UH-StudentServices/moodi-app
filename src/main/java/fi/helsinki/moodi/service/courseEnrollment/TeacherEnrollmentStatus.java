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
