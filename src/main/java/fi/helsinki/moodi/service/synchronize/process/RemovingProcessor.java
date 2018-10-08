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

import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor implementation that removes course from the Moodi database.
 */
@Component
public class RemovingProcessor extends AbstractProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RemovingProcessor.class);

    private final CourseService courseService;

    @Autowired
    public RemovingProcessor(CourseService courseService) {
        super(Action.REMOVE);
        this.courseService = courseService;
    }

    @Override
    protected SynchronizationItem doProcess(final SynchronizationItem item) {
        courseService.markAsRemoved(item.getCourse(), item.getEnrichmentStatus().toString());
        return item.completeProcessingPhase(ProcessingStatus.SUCCESS, "Removed", true);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
