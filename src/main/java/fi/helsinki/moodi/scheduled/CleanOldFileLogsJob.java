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

package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.service.synchronize.log.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that cleans synchronization summary history.
 */
@Component
public class CleanOldFileLogsJob {

    private final LoggingService loggingService;

    @Autowired
    public CleanOldFileLogsJob(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    // Run daily 6 o'clock
    @Scheduled(cron = "0 1 6 * * ?")
    public void execute() {
        loggingService.cleanOldLogs();
    }
}
