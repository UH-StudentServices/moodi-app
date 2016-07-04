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
