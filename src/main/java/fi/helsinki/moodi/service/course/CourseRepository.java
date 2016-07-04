package fi.helsinki.moodi.service.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByRealisationId(long realisationId);

    List<Course> findByImportStatusIn(List<ImportStatus> importStatus);

    List<Course> findByImportStatusInAndRealisationIdIn(List<ImportStatus> importStatuses, List<Long> realisationIds);

}
