package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class EnricherService {

    private final EnrichExecutor enrichExecutor;

    @Autowired
    public EnricherService(EnrichExecutor enrichExecutor) {
        this.enrichExecutor = enrichExecutor;
    }

    private SynchronizationItem readItem(Future<SynchronizationItem> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new EnrichException("Error reading synchronization items from future", e);
        }
    }

    public List<SynchronizationItem> enrich(final List<SynchronizationItem> items) {
        return items.stream()
            .map(enrichExecutor::enrichItem)
            .map(this::readItem)
            .collect(Collectors.toList());
    }

}
