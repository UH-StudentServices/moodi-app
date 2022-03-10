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

import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MoodleEnrollmentsEnricher extends AbstractEnricher {

    private static final Logger logger = LoggerFactory.getLogger(MoodleEnrollmentsEnricher.class);

    private final MoodleService moodleService;

    @Autowired
    public MoodleEnrollmentsEnricher(MoodleService moodleService) {
        super(3);
        this.moodleService = moodleService;
    }

    @Override
    protected SynchronizationItem doEnrich(final SynchronizationItem item) {
        final Course course = item.getCourse();
        final List<MoodleUserEnrollments> moodleEnrollments = moodleService.getEnrolledUsers(course.moodleId);
        return item.setMoodleEnrollments(Optional.of(moodleEnrollments));
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
