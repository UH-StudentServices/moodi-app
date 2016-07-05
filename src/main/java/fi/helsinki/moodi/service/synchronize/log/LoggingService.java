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

package fi.helsinki.moodi.service.synchronize.log;

import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that orchestrates logging.
 */
@Service
public class LoggingService {

    private final List<SynchronizationSummaryLogger> loggers;

    @Autowired
    public LoggingService(List<SynchronizationSummaryLogger> loggers) {
        this.loggers = loggers;
    }

    public void logSynchronizationSummary(final SynchronizationSummary summary) {
        for (final SynchronizationSummaryLogger logger : loggers) {
            logger.log(summary);
        }
    }

    public void cleanOldLogs() {
        for (final SynchronizationSummaryLogger logger : loggers) {
            logger.cleanOldLogs();
        }
    }
}
