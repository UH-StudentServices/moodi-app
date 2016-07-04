package fi.helsinki.moodi.service.courseEnrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseEnrollmentStatusRepository extends JpaRepository<CourseEnrollmentStatus, Long> {
    Optional<CourseEnrollmentStatus> findTop1ByRealisationIdOrderByCreatedDesc(Long realisationId);
}
