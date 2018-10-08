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

import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MoodleCourseEnricher extends AbstractEnricher {

    private static final Logger logger = LoggerFactory.getLogger(MoodleCourseEnricher.class);

    private final MoodleService moodleService;

    @Autowired
    public MoodleCourseEnricher(MoodleService moodleService) {
        super(2);
        this.moodleService = moodleService;
    }

    @Override
    protected SynchronizationItem doEnrich(final SynchronizationItem item) {
        final Course course = item.getCourse();
        final List<MoodleFullCourse> courses = moodleService.getCourses(Lists.newArrayList(course.moodleId));

        if (courses.isEmpty()) {
            return item.completeEnrichmentPhase(
                    EnrichmentStatus.MOODLE_COURSE_NOT_FOUND,
                    "Course not found from Moodle with id " + course.moodleId);
        } else {
            return item.setMoodleCourse(courses.stream().findFirst());
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}