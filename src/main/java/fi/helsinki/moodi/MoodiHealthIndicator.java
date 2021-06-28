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

package fi.helsinki.moodi;

import fi.helsinki.moodi.scheduled.FullSynchronizationJob;
import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRun;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class MoodiHealthIndicator implements HealthIndicator {
    private static final int MAX_HOURS_SINCE_COMPLETED_SYNC = 3;
    private static final int MIN_MINUTES_SINCE_ERROR = 30;
    private Error reportedError;

    private final SynchronizationJobRunService synchronizationJobRunService;
    private final TimeService timeService;
    private final FullSynchronizationJob fullSynchronizationJob;

    public MoodiHealthIndicator(SynchronizationJobRunService synchronizationJobRunService, TimeService timeService,
                                FullSynchronizationJob fullSynchronizationJob) {
        this.synchronizationJobRunService = synchronizationJobRunService;
        this.timeService = timeService;
        this.fullSynchronizationJob = fullSynchronizationJob;
    }

    // Return DOWN, if
    // - a sync job has not completed SUCCESSFULLY within 3 hours.
    // - an important API action has failed within 30 minutes, and has not succeeded since.
    @Override
    public Health health() {
        LocalDateTime now = timeService.getCurrentUTCDateTime(); // DB timestamps are in UTC.
        try {
            if (fullSynchronizationJob.isEnabled()) {
                Optional<SynchronizationJobRun> searched = synchronizationJobRunService.findLatestCompletedJob();
                if (searched.isPresent()) {
                    SynchronizationJobRun latest = searched.get();
                    if (latest.completed == null || latest.completed.isBefore(now.minusHours(MAX_HOURS_SINCE_COMPLETED_SYNC))) {
                        return indicateError(String.format("No sync job completed in %d hours. Latest job completed at %s UTC",
                            MAX_HOURS_SINCE_COMPLETED_SYNC, latest.completed));
                    } else if (latest.status != SynchronizationStatus.COMPLETED_SUCCESS) {
                        return indicateError(String.format("Status of the last sync job is not COMPLETED_SUCCESS, but %s", latest.message));
                    }
                } else {
                    return indicateError("No sync jobs in the DB.");
                }
            }

            if (reportedError != null && reportedError.when.isAfter(now.minusMinutes(MIN_MINUTES_SINCE_ERROR))) {
                return indicateError(reportedError.message, reportedError.exception);
            }
        } catch (Exception e) {
            return indicateError(e.toString());
        }
        return Health.up().build();
    }

    public void reportError(String error, Exception e) {
        reportedError = new Error(error, e);
    }

    public void clearError() {
        reportedError = null;
    }

    private Health indicateError(String message) {
        return indicateError(message, null);
    }

    private Health indicateError(String message, Exception e) {
        return Health.down()
            .withDetail("error", message)
            .withDetail("stack", e != null ? stackString(e) : "none")
            .build();
    }

    private Object stackString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private class Error {
        public String message;
        public Exception exception;
        public LocalDateTime when;

        public Error(String error, Exception e) {
            this.message = error;
            this.exception = e;
            this.when = timeService.getCurrentUTCDateTime(); // This class uses UTC, because DB timestamps are in UTC.
        }
    }
}
