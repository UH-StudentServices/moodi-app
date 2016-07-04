package fi.helsinki.moodi.service.synchronize.job;

import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SynchronizationJobRunRepository extends JpaRepository<SynchronizationJobRun, Long> {

    Optional<SynchronizationJobRun> findFirstByTypeAndStatusInOrderByCompletedDesc(
            SynchronizationType type, List<SynchronizationStatus> status);

    Optional<SynchronizationJobRun> findFirstByTypeOrderByCompletedDesc(SynchronizationType type);

    @Modifying
    @Query("delete from #{#entityName} where type = ?1 and started < ?2")
    void deleteByTypeAndDate(SynchronizationType type, LocalDateTime date);

}
