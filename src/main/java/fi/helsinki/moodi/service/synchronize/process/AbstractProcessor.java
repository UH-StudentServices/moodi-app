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

package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.exception.ProcessingException;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;

/**
 * Abstract {@link Processor} base class.
 */
abstract class AbstractProcessor implements Processor {

    private final Action action;

    protected AbstractProcessor(Action action) {
        this.action = action;
    }

    @Override
    public final SynchronizationItem process(final SynchronizationItem item) {
        if (item.getProcessingStatus() != ProcessingStatus.IN_PROGRESS) {
            getLogger().debug("Item already processed, just return it");
            return item;
        } else {
            return safeProces(item);
        }
    }

    private SynchronizationItem safeProces(final SynchronizationItem item) {
        try {
            return doProcess(item);
        } catch (ProcessingException e) {
            return synchronizationError(item, e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return synchronizationError(item, ProcessingStatus.ERROR, e.getMessage());
        }
    }

    private SynchronizationItem synchronizationError(SynchronizationItem item, ProcessingStatus status, String message) {
        getLogger().error("Error while processing item", message);
        return item.completeProcessingPhase(status, message);
    }

    @Override
    public final Action getAction() {
        return action;
    }

    protected abstract SynchronizationItem doProcess(SynchronizationItem item);

    protected abstract Logger getLogger();

}
