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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

@Service
public class EnricherService {

    private final List<Enricher> enrichers;

    @Autowired
    public EnricherService(List<Enricher> enrichers) {
        this.enrichers = enrichers;
        enrichers.sort(Comparator.comparingInt(Ordered::getOrder));
    }

    public SynchronizationItem enrichItem(final SynchronizationItem item) {
        return applyEnricher(item, 0);
    }

    private SynchronizationItem applyEnricher(final SynchronizationItem item, final int index) {
        if (index < enrichers.size()) {
            SynchronizationItem enrichedItem;
            try {
                enrichedItem = enrichers.get(index).enrich(item);
            } catch (Exception e) {
                throw new EnrichException("Error enriching synchronization item", e);
            }
            return applyEnricher(enrichedItem, index + 1);
        } else {
            return item;
        }
    }
}
