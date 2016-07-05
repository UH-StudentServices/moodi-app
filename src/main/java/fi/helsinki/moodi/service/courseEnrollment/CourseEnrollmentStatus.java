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

import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_enrollment_status")
public class CourseEnrollmentStatus {
    @Id
    @SequenceGenerator(name = "course_enrollment_status_generator", sequenceName = "course_enrollment_status_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_enrollment_status_generator")
    public Long id;

    @NotNull
    @Column(name = "course_id")
    public Long courseId;

    @NotNull
    @Column(name = "course_realisation_id")
    public long realisationId;

    @Column(name = "student_enrollments")
    public String studentEnrollments;

    @Column(name = "teacher_enrollments")
    public String teacherEnrollments;

    @Column(name = "created")
    @NotNull
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentLocalDateTime")
    public LocalDateTime created;

}
