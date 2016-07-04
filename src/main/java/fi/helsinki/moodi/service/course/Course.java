package fi.helsinki.moodi.service.course;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "course")
public class Course implements Serializable {
    
    public enum ImportStatus {
        IN_PROGRESS,
        COMPLETED,
        COMPLETED_FAILED
    }

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "course_id_seq_generator", sequenceName = "course_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_id_seq_generator")
    public Long id;

    @Column(name = "realisation_id")
    @NotNull
    public long realisationId;

    @Column(name = "moodle_id")
    @NotNull
    public long moodleId;

    @Column(name = "created")
    @NotNull
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentLocalDateTime")
    public LocalDateTime created;

    @Column(name = "import_status")
    @NotNull
    @Enumerated(EnumType.STRING)
    public ImportStatus importStatus;

}
