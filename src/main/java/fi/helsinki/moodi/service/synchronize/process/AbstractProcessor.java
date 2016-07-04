package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;

/**
 * Abstract {@link Processor} base class.
 */
abstract class AbstractProcessor implements Processor {

    private final Action action;

    protected AbstractProcessor(Action action) {
        this.action = action;
    }

    @Override
    public final SynchronizationItem process(final SynchronizationItem item) {
        if (item.getProcessingStatus() != ProcessingStatus.IN_PROGRESS) {
            getLogger().debug("Item already processed, just return it");
            return item;
        } else {
            return safeProces(item);
        }
    }

    private SynchronizationItem safeProces(final SynchronizationItem item) {
        try {
            return doProcess(item);
        } catch (Exception e) {
            getLogger().error("Error while processing item", e);
            return item.completeProcessingPhase(ProcessingStatus.ERROR, e.getMessage());
        }
    }

    @Override
    public final Action getAction() {
        return action;
    }

    protected abstract SynchronizationItem doProcess(SynchronizationItem item);

    protected abstract Logger getLogger();

}
