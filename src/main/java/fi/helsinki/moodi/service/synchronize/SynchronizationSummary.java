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

package fi.helsinki.moodi.service.synchronize;

import com.google.common.base.Stopwatch;

import java.util.List;

import static fi.helsinki.moodi.service.synchronize.SynchronizationStatus.COMPLETED_FAILURE;
import static fi.helsinki.moodi.service.synchronize.SynchronizationStatus.COMPLETED_SUCCESS;

/**
 * Summary of synchronization run.
 */
public final class SynchronizationSummary {

    private final SynchronizationType type;
    private final List<SynchronizationItem> items;
    private final Stopwatch stopwatch;

    public SynchronizationSummary(SynchronizationType type, List<SynchronizationItem> items, Stopwatch stopwatch) {
        this.type = type;
        this.items = items;
        this.stopwatch = stopwatch;
    }

    public SynchronizationStatus getStatus() {
        return (getItemCount() == getSuccessfulItemsCount()) ? COMPLETED_SUCCESS : COMPLETED_FAILURE;
    }
    public SynchronizationType getType() {
        return type;
    }

    public long getItemCount() {
        return items.size();
    }

    public String getElapsedTime() {
        return stopwatch.toString();
    }

    public long getSuccessfulItemsCount() {
        return items.stream().filter(SynchronizationItem::isSuccess).count();
    }

    public long getFailedItemsCount() {
        return getItemCount() - getSuccessfulItemsCount();
    }

    public String getMessage() {
        return getStatus().name();
    }

    public List<SynchronizationItem> getItems() {
        return items;
    }
}
