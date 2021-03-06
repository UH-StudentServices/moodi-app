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

import com.google.common.collect.Lists;
import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static fi.helsinki.moodi.service.synchronize.SynchronizationStatus.*;
import static fi.helsinki.moodi.service.synchronize.SynchronizationType.INCREMENTAL;

@Service
@Transactional
public class SynchronizationJobRunService {

    private final SynchronizationJobRunRepository synchronizationJobRunRepository;
    private final TimeService timeService;
    private final Environment environment;

    @Autowired
    public SynchronizationJobRunService(
            SynchronizationJobRunRepository synchronizationJobRunRepository,
            TimeService timeService,
            Environment environment) {
        this.synchronizationJobRunRepository = synchronizationJobRunRepository;
        this.timeService = timeService;
        this.environment = environment;
    }

    @PostConstruct
    public void checkForInterruptedRuns() {
        List<SynchronizationJobRun> interruptedRuns = synchronizationJobRunRepository.findByStatus(SynchronizationStatus.STARTED);
        interruptedRuns.stream().forEach(run -> run.status = SynchronizationStatus.INTERRUPTED);
        synchronizationJobRunRepository.saveAll(interruptedRuns);
    }

    public boolean isSynchronizationInProgress() {
        List<SynchronizationJobRun> inProgressRuns = synchronizationJobRunRepository.findByStatus(SynchronizationStatus.STARTED);
        return inProgressRuns.size() > 0;
    }

    public Optional<SynchronizationJobRun> findLatestCompletedIncrementalJob() {
        return synchronizationJobRunRepository.findFirstByTypeAndStatusInOrderByCompletedDesc(
            INCREMENTAL,
            Lists.newArrayList(COMPLETED_SUCCESS, COMPLETED_FAILURE));
    }

    public Optional<SynchronizationJobRun> findLatestJob(SynchronizationType type) {
        return synchronizationJobRunRepository.findFirstByTypeOrderByCompletedDesc(type);
    }

    public Optional<SynchronizationJobRun> findLatestCompletedJob() {
        return synchronizationJobRunRepository.findFirstByStatusInOrderByCompletedDesc(Lists.newArrayList(COMPLETED_SUCCESS, COMPLETED_FAILURE));
    }

    public long begin(final SynchronizationType type) {
        final SynchronizationJobRun job = new SynchronizationJobRun();
        job.message = "Started";
        job.started = timeService.getCurrentUTCDateTime();
        job.status = STARTED;
        job.type = type;

        return synchronizationJobRunRepository.saveAndFlush(job).id;
    }

    public void complete(final Long id, final SynchronizationStatus status, final String message) {
        final SynchronizationJobRun job = synchronizationJobRunRepository
            .findById(id)
            .orElseThrow(RuntimeException::new);

        job.status = status;
        job.message = message;
        job.completed = timeService.getCurrentUTCDateTime();

        synchronizationJobRunRepository.save(job);
    }

    public void cleanOldRuns() {
        for (final SynchronizationType type : SynchronizationType.values()) {
            final String retainLogsDurationString = environment.getRequiredProperty("logging.retain-logs");
            final Duration duration = Duration.parse(retainLogsDurationString);
            final LocalDateTime date = timeService.getCurrentUTCDateTime().minusSeconds(duration.getSeconds());
            synchronizationJobRunRepository.deleteByTypeAndDate(type, date);
        }
    }
}
