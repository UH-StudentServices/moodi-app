package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;

abstract class AbstractEnricher implements Enricher {

    private final int order;

    protected AbstractEnricher(int order) {
        this.order = order;
    }

    @Override
    public final SynchronizationItem enrich(final SynchronizationItem item) {
        if (item.getEnrichmentStatus() != EnrichmentStatus.IN_PROGESS) {
            getLogger().debug("Item enrichment already completed, just return it");
            return item;
        } else {
            return safeEnrich(item);
        }
    }

    @Override
    public final int getOrder() {
        return order;
    }

    private SynchronizationItem safeEnrich(final SynchronizationItem item) {
        try {
            return doEnrich(item);
        } catch (Exception e) {
            getLogger().error("Error while enriching item", e);
            return item.completeEnrichmentPhase(EnrichmentStatus.ERROR, e.getMessage());
        }
    }

    protected abstract SynchronizationItem doEnrich(SynchronizationItem item);

    protected abstract Logger getLogger();
}
