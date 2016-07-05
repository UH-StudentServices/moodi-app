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
