package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static fi.helsinki.moodi.service.synchronize.SynchronizationType.INCREMENTAL;

/**
 * Scheduled job that performs full synchronization
 * to all Moodle courses create by Moodi.
 */
@Component
public class IncrementalSynchronizationJob {

    private final Environment environment;
    private final SynchronizationService synchronizationService;

    @Autowired
    public IncrementalSynchronizationJob(Environment environment, SynchronizationService synchronizationService) {
        this.environment = environment;
        this.synchronizationService = synchronizationService;
    }

    // Run every 2 hours
    @Scheduled(initialDelay = 60000, fixedDelay = 7200000)
    public void execute() {
        if (isEnabled()) {
            synchronizationService.synchronize(INCREMENTAL);
        }
    }

    private boolean isEnabled() {
        final String key = String.format("synchronize.%s.enabled", INCREMENTAL);
        return environment.getRequiredProperty(key, Boolean.class);
    }
}
