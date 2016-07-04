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
