package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.springframework.core.Ordered;

public interface Enricher extends Ordered {

    SynchronizationItem enrich(final SynchronizationItem item);
}
