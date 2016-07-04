package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.service.synchronize.log.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that cleans synchronization summary history.
 */
@Component
public class CleanSynchronizationSummaryHistoryJob {

    private final LoggingService loggingService;

    @Autowired
    public CleanSynchronizationSummaryHistoryJob(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    // Run daily 6 o'clock
    @Scheduled(cron = "0 1 6 * * ?")
    public void execute() {
        loggingService.cleanOldLogs();
    }
}
