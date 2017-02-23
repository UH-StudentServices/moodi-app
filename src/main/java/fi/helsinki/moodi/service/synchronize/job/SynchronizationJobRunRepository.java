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

    List<SynchronizationJobRun> findByStatus(SynchronizationStatus status);

    Optional<SynchronizationJobRun> findFirstByTypeOrderByCompletedDesc(SynchronizationType type);

    @Modifying
    @Query("delete from #{#entityName} where type = ?1 and started < ?2")
    void deleteByTypeAndDate(SynchronizationType type, LocalDateTime date);

}
