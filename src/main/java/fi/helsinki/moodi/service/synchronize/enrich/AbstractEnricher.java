/*
 * This file is part of Moodi application.
 *
 * Moodi application is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moodi application is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Moodi application.  If not, see <http://www.gnu.org/licenses/>.
 */

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
