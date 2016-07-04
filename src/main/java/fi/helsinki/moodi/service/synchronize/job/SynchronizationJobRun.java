package fi.helsinki.moodi.service.synchronize.job;

import javax.persistence.*;

import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "synchronization_job_run")
public class SynchronizationJobRun implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "synchronization_job_run_id_seq_generator", sequenceName = "synchronization_job_run_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "synchronization_job_run_id_seq_generator")
    public Long id;

    @Column(name = "message")
    @NotNull
    public String message;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    public SynchronizationStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @NotNull
    public SynchronizationType type;

    @Column(name = "started")
    @NotNull
    @org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentLocalDateTime")
    public LocalDateTime started;

    @Column(name = "completed")
    @org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentLocalDateTime")
    public LocalDateTime completed;
}
