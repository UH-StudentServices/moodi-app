package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static fi.helsinki.moodi.service.synchronize.SynchronizationType.FULL;

/**
 * Scheduled job that performs full synchronization
 * to all Moodle courses create by Moodi.
 */
@Component
public class FullSynchronizationJob {

    private final Environment environment;
    private final SynchronizationService synchronizationService;

    @Autowired
    public FullSynchronizationJob(Environment environment, SynchronizationService synchronizationService) {
        this.environment = environment;
        this.synchronizationService = synchronizationService;
    }

    // Run daily 1 o'clock
    @Scheduled(cron = "0 1 1 * * ?")
    public void execute() {
        if (isEnabled()) {
            synchronizationService.synchronize(FULL);
        }
    }

    private boolean isEnabled() {
        final String key = String.format("synchronize.%s.enabled", FULL);
        return environment.getRequiredProperty(key, Boolean.class);
    }
}
