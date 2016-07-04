package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;

@Component
public class EnrichExecutor {

    private final List<Enricher> enrichers;

    @Autowired
    public EnrichExecutor(List<Enricher> enrichers) {
        this.enrichers = enrichers;
        enrichers.sort((a,b) -> Integer.compare(a.getOrder(), b.getOrder()));
    }

    @Async("taskExecutor")
    public Future<SynchronizationItem> enrichItem(final SynchronizationItem item) {
        return new AsyncResult<>(applyEnricher(item, 0));
    }

    private SynchronizationItem applyEnricher(final SynchronizationItem item, final int index) {
        if (index < enrichers.size()) {
            final SynchronizationItem enrichedItem = enrichers.get(index).enrich(item);
            return applyEnricher(enrichedItem, index + 1);
        } else {
            return item;
        }
    }

}
