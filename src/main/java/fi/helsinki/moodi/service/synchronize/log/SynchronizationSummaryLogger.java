package fi.helsinki.moodi.service.synchronize.log;

import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;

/**
 * Log synchronization summary.
 */
public interface SynchronizationSummaryLogger {

    void log(SynchronizationSummary summary);

    void cleanOldLogs();
}
