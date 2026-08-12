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
import fi.helsinki.moodi.exception.SynchronizationInProgressException;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.log.LoggingService;
import fi.helsinki.moodi.service.synchronize.enrich.EnricherService;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import fi.helsinki.moodi.service.synchronize.notify.SynchronizationItemNotifier;
import fi.helsinki.moodi.service.synchronize.process.ProcessorService;
import fi.helsinki.moodi.service.synclock.SyncLockService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service that orchestrates synchronization runs.
 */
@Service
public class SynchronizationService {

    private static final Logger logger = getLogger(SynchronizationService.class);

    private final EnricherService enricherService;
    private final ProcessorService processorService;
    private final SynchronizationJobRunService synchronizationJobRunService;
    private final LoggingService loggingService;
    private final CourseService courseService;
    private final SyncLockService syncLockService;
    private final List<SynchronizationItemNotifier> notifiers;

    @Autowired
    public SynchronizationService(
        EnricherService enricherService,
        ProcessorService processorService,
        SynchronizationJobRunService synchronizationJobRunService,
        LoggingService loggingService,
        CourseService courseService,
        SyncLockService syncLockService,
        List<SynchronizationItemNotifier> notifiers) {

        this.enricherService = enricherService;
        this.processorService = processorService;
        this.synchronizationJobRunService = synchronizationJobRunService;
        this.loggingService = loggingService;
        this.courseService = courseService;
        this.syncLockService = syncLockService;
        this.notifiers = notifiers;
    }

    public SynchronizationSummary synchronize(final SynchronizationType type) {

        if (synchronizationJobRunService.isSynchronizationInProgress()) {
            throw new SynchronizationInProgressException(type);
        }

        final Stopwatch stopwatch = Stopwatch.createStarted();
        final long jobId  = begin(type);
        final List<SynchronizationItem> processedItems = new ArrayList<>();
        SynchronizationSummary summary;
        Exception exception = null;
        try {
            logger.info("Synchronization of type {} started with jobId {}", type, jobId);

            final List<Course> courses = loadCourses(type);
            final List<SynchronizationItem> items = makeItems(courses, type);
            final List<SynchronizationItem> enrichedItems = enricherService.enrichItems(items);
            processedItems.addAll(processorService.process(enrichedItems));
        } catch (Exception e) {
            logger.error("Exception in SynchronizationService", e);
            exception = e;
        } finally {
            summary = complete(type, jobId, stopwatch, processedItems, exception);
        }

        logger.info("Synchronization with jobId {} completed in {}", jobId, stopwatch);

        applyNotifiers(processedItems);

        return logSummary(summary);
    }

    private long begin(final SynchronizationType type) {
        return synchronizationJobRunService.begin(type);
    }

    /**
     * Load courses to be synchronized.
     */
    private List<Course> loadCourses(final SynchronizationType type) {
        switch (type) {
            case FULL:
            case RECONCILE:
                return courseService.findAllCompletedWithMoodleId();
            case UNLOCK:
                return syncLockService.getAndUnlockLockedCourses();
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Convert courses into synchronization items to be enriched with
     * data required to perform the actual synchronization.
     */
    private List<SynchronizationItem> makeItems(final List<Course> courses, SynchronizationType type) {
        return courses.stream().map(c -> new SynchronizationItem(c, type)).collect(toList());
    }

    private SynchronizationSummary complete(
            final SynchronizationType type, final long jobId, final Stopwatch stopwatch, final List<SynchronizationItem> items, Exception exception) {

        final SynchronizationSummary summary = new SynchronizationSummary(type, items, stopwatch.stop(), exception);
        synchronizationJobRunService.complete(jobId, summary.getStatus(), summary.getMessage());
        return summary;
    }

    private void applyNotifiers(final List<SynchronizationItem> items) {
        notifiers.forEach(notifier -> notifier.applyNotificationsForItems(items));
    }

    private SynchronizationSummary logSummary(final SynchronizationSummary summary) {
        loggingService.logSynchronizationSummary(summary);
        return summary;
    }
}
