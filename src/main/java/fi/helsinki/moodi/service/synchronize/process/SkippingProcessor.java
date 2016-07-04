package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Processor implementation that skips the item.
 */
@Component
public class SkippingProcessor extends AbstractProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkippingProcessor.class);

    public SkippingProcessor() {
        super(Action.SKIP);
    }

    @Override
    protected SynchronizationItem doProcess(final SynchronizationItem item) {
        return item.completeProcessingPhase(ProcessingStatus.SKIPPED, "Can't synchronize");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
