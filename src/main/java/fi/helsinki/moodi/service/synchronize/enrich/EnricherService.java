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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class EnricherService {

    private static final Logger logger = LoggerFactory.getLogger(EnricherService.class);

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
