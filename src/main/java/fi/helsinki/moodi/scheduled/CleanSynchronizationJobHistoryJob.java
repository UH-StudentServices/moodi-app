package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that cleans synchronization job history.
 */
@Component
public class CleanSynchronizationJobHistoryJob {

    private final SynchronizationJobRunService synchronizationJobRunService;

    @Autowired
    public CleanSynchronizationJobHistoryJob(SynchronizationJobRunService synchronizationJobRunService) {
        this.synchronizationJobRunService = synchronizationJobRunService;
    }

    // Run daily 5 o'clock
    @Scheduled(cron = "0 1 5 * * ?")
    public void execute() {
        synchronizationJobRunService.cleanOldRuns();
    }
}
