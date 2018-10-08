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

import fi.helsinki.moodi.service.synclock.SyncLockService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LockStatusEnricher extends AbstractEnricher {

    private static final Logger logger = LoggerFactory.getLogger(LockStatusEnricher.class);

    private final SyncLockService syncLockService;

    @Autowired
    public LockStatusEnricher(SyncLockService syncLockService) {
        super(0);
        this.syncLockService = syncLockService;
    }

    @Override
    protected SynchronizationItem doEnrich(SynchronizationItem item) {
        final boolean isLocked = syncLockService.isLocked(item.getCourse());

        if (SynchronizationType.UNLOCK.equals(item.getSynchronizationType())) {
            return item.setUnlock(true);
        } else if (isLocked) {
            return item.completeEnrichmentPhase(EnrichmentStatus.LOCKED, "Item locked. Will not synchronize.");
        } else {
            return item;
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
