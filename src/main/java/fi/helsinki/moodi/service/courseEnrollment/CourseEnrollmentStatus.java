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
