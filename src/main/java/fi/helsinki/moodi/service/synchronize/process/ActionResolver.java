package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;

/**
 * Resolves which {@link Action} to perform for {@link SynchronizationItem}
 * after enrichment phase.
 *
 * @see Action
 * @see SynchronizationItem
 */
public final class ActionResolver {

    public static Action resolve(final SynchronizationItem item) {

        switch (item.getEnrichmentStatus()) {
            case SUCCESS:
                return Action.SYNCHRONIZE;
            case MOODLE_COURSE_NOT_FOUND:
            case OODI_COURSE_NOT_FOUND:
            case OODI_COURSE_ENDED:
                return Action.REMOVE;
            default:
                return Action.SKIP;
        }
    }
}
