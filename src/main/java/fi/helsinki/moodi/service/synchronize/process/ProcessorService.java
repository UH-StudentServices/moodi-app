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

import com.google.common.collect.Lists;
import fi.helsinki.moodi.exception.ProcessingException;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

/**
 * Process item by either skipping, removing or synchronizing it.
 */
@Service
public class ProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorService.class);
    private final CourseService courseService;
    private final SynchronizingProcessor synchronizingProcessor;

    @Autowired
    public ProcessorService(CourseService courseService, SynchronizingProcessor synchronizingProcessor) {
        this.courseService = courseService;
        this.synchronizingProcessor = synchronizingProcessor;
    }

    private SynchronizationItem synchronizationError(SynchronizationItem item, ProcessingStatus status, Exception e) {
        logger.error("Error while processing item: ", e);
        item.completeProcessingPhase(status, e.getMessage());
        return item;
    }

    private boolean completed(final SynchronizationItem item) {
        if (item.getProcessingStatus() != ProcessingStatus.IN_PROGRESS) {
            logger.debug("Item already processed, just return it");
            return true;
        }
        return false;
    }

    private SynchronizationItem synchronizeItem(SynchronizationItem item) {
        if (completed(item)) {
            return item;
        }
        try {
            item = synchronizingProcessor.doSynchronize(item);
        } catch (ProcessingException e) {
            return synchronizationError(item, e.getStatus(), e);
        } catch (Exception e) {
            return synchronizationError(item, ProcessingStatus.ERROR, e);
        }
        return item;
    }


    private SynchronizationItem removeItem(final SynchronizationItem item) {
        if (completed(item)) {
            return item;
        }
        try {
            courseService.markAsRemoved(item.getCourse(), item.getEnrichmentStatus().toString());
            item.completeProcessingPhase(ProcessingStatus.SUCCESS, "Removed", true);
        } catch (ProcessingException e) {
            return synchronizationError(item, e.getStatus(), e);
        } catch (Exception e) {
            return synchronizationError(item, ProcessingStatus.ERROR, e);
        }
        return item;
    }

    private SynchronizationItem skipItem(final SynchronizationItem item) {
        if (completed(item)) {
            return item;
        }
        try {
            item.completeProcessingPhase(ProcessingStatus.SKIPPED, "Can't synchronize");
        } catch (ProcessingException e) {
            return synchronizationError(item, e.getStatus(), e);
        } catch (Exception e) {
            return synchronizationError(item, ProcessingStatus.ERROR, e);
        }
        return item;
    }

    public List<SynchronizationItem> process(final List<SynchronizationItem> items) {
        final Map<Action, List<SynchronizationItem>> itemsByAction = groupItemsByAction(items);
        final List<SynchronizationItem> processedItems = Lists.newArrayList();

        itemsByAction.get(Action.SKIP).forEach(item -> {
            try {
                item = skipItem(item);
            } catch (Exception e) {
                throw new ProcessException("Error processing item (SKIPPING) " + item.toString(), e);
            }
            processedItems.add(item);
        });
        itemsByAction.get(Action.REMOVE).forEach(item -> {
            try {
                item = removeItem(item);
            } catch (Exception e) {
                throw new ProcessException("Error processing item (REMOVING) " + item.toString(), e);
            }
            processedItems.add(item);
        });
        itemsByAction.get(Action.SYNCHRONIZE).forEach(item -> {
            try {
                item = synchronizeItem(item);
            } catch (Exception e) {
                throw new ProcessException("Error processing item (SYNCHRONIZING) " + item.toString(), e);
            }
            processedItems.add(item);
        });
        return processedItems;
    }

    private Map<Action, List<SynchronizationItem>> groupItemsByAction(final List<SynchronizationItem> items) {
        return items.stream().collect(groupingBy(this::resolveAction));
    }

    private Action resolveAction(final SynchronizationItem item) {
        switch (item.getEnrichmentStatus()) {
            case SUCCESS:
                return Action.SYNCHRONIZE;
            case COURSE_NOT_PUBLIC:
            case COURSE_ENDED:
            case MOODLE_COURSE_NOT_FOUND:
                return Action.REMOVE;
            case LOCKED:
            default:
                return Action.SKIP;
        }
    }
}
