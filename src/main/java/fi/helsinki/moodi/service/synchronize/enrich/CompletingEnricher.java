package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CompletingEnricher extends AbstractEnricher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletingEnricher.class);

    protected CompletingEnricher() {
        super(30);
    }

    @Override
    protected SynchronizationItem doEnrich(SynchronizationItem item) {
        return item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "Enrichment successfull");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
