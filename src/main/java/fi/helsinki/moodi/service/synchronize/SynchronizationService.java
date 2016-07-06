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
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.enrich.EnricherService;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import fi.helsinki.moodi.service.synchronize.loader.CourseLoaderService;
import fi.helsinki.moodi.service.synchronize.log.LoggingService;
import fi.helsinki.moodi.service.synchronize.process.ProcessorService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service that orchestrates synchronization runs.
 */
@Service
public class SynchronizationService {

    private static final Logger LOGGER = getLogger(SynchronizationService.class);

    private final EnricherService enricherService;
    private final ProcessorService processorService;
    private final SynchronizationJobRunService synchronizationJobRunService;
    private final CourseLoaderService courseLoaderService;
    private final LoggingService loggingService;


    @Autowired
    public SynchronizationService(
            EnricherService enricherService,
            ProcessorService processorService,
            SynchronizationJobRunService synchronizationJobRunService,
            CourseLoaderService courseLoaderService,
            LoggingService loggingService) {

        this.enricherService = enricherService;
        this.processorService = processorService;
        this.synchronizationJobRunService = synchronizationJobRunService;
        this.courseLoaderService = courseLoaderService;
        this.loggingService = loggingService;
    }

    public SynchronizationSummary synchronize(final SynchronizationType type) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final long jobId  = begin(type);

        final List<Course> courses = loadCourses(type);
        final List<SynchronizationItem> items = makeItems(courses);
        final List<SynchronizationItem> enrichedItems = enrichItems(items);
        final List<SynchronizationItem> processedItems = processItems(enrichedItems);

        final SynchronizationSummary summary = complete(type, jobId, stopwatch, processedItems);

        return logSummary(summary);
    }

    private long begin(final SynchronizationType type) {
        return synchronizationJobRunService.begin(type);
    }

    /**
     * Load courses to be synchronized.
     */
    private List<Course> loadCourses(final SynchronizationType type) {
        return courseLoaderService.load(type);
    }

    /**
     * Convert courses into synchronization items to be enriched with
     * data required to perform the actual synchronization.
     */
    private List<SynchronizationItem> makeItems(final List<Course> courses) {
        return courses.stream().map(SynchronizationItem::new).collect(toList());
    }

    /**
     * Enrich items with data required in synchronization.
     */
    private List<SynchronizationItem> enrichItems(final List<SynchronizationItem> items) {
        return enricherService.enrich(items);
    }

    /**
     * Process items, that is perform the actual synchronization.
     */
    private List<SynchronizationItem> processItems(final List<SynchronizationItem> items) {
        return processorService.process(items);
    }

    private SynchronizationSummary complete(
            final SynchronizationType type, final long jobId, final Stopwatch stopwatch, final List<SynchronizationItem> items) {

        final SynchronizationSummary summary = new SynchronizationSummary(type, items, stopwatch.stop());
        synchronizationJobRunService.complete(jobId, summary.getStatus(), summary.getMessage());
        return summary;
    }

    private SynchronizationSummary logSummary(final SynchronizationSummary summary) {
        loggingService.logSynchronizationSummary(summary);
        return summary;
    }
}
