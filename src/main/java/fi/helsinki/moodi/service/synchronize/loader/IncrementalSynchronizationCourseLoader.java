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

package fi.helsinki.moodi.service.synchronize.loader;

import fi.helsinki.moodi.integration.oodi.OodiClient;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRun;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import fi.helsinki.moodi.service.time.SystemTimeService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation for incremental synchronization.
 */
@Component
public class IncrementalSynchronizationCourseLoader implements CourseLoader {

    private static final Logger logger = getLogger(IncrementalSynchronizationCourseLoader.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CourseService courseService;
    private final SynchronizationJobRunService synchronizationJobRunService;
    private final SystemTimeService timeService;
    private final OodiClient oodiService;

    @Autowired
    public IncrementalSynchronizationCourseLoader(
            CourseService courseService,
            SynchronizationJobRunService synchronizationJobRunService,
            SystemTimeService timeService,
            OodiClient oodiService) {

        this.courseService = courseService;
        this.synchronizationJobRunService = synchronizationJobRunService;
        this.timeService = timeService;
        this.oodiService = oodiService;
    }

    @Override
    public List<Course> load() {
        final Optional<SynchronizationJobRun> lastRun =
                synchronizationJobRunService.findLatestCompletedIncrementalJob();
        final LocalDateTime afterDate =
                lastRun.map(s -> s.completed).orElse(timeService.getCurrentUTCDateTime().minusDays(1));

        logger.debug("Last successful synchronization run at {}", FORMATTER.format(afterDate));

        final List<Long> realisationIds = oodiService.getCourseChanges(afterDate)
                .stream()
                .map(o -> o.courseUnitRealisationId)
                .collect(Collectors.toList());

        return courseService.findCompletedByRealisationIds(realisationIds);
    }

    @Override
    public SynchronizationType getType() {
        return SynchronizationType.INCREMENTAL;
    }
}
