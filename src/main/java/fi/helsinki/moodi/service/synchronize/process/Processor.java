package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;

/**
 * Performs {@link Action} against {@link SynchronizationItem}.
 *
 * @see Action
 */
public interface Processor {

    /**
     * Process item and return processed item.
     */
    SynchronizationItem process(SynchronizationItem item);

    /**
     * @return What action this processor can perform
     */
    Action getAction();
}
