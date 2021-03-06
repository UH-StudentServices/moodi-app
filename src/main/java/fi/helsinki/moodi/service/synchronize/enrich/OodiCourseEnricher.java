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

import fi.helsinki.moodi.integration.oodi.BaseOodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiClient;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryService;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static fi.helsinki.moodi.util.DateFormat.OODI_UTC_DATE_FORMAT;

@Component
public class OodiCourseEnricher extends AbstractEnricher {

    private static final Logger logger = LoggerFactory.getLogger(OodiCourseEnricher.class);

    private final OodiClient oodiClient;

    @Autowired
    public OodiCourseEnricher(OodiClient oodiClient) {
        super(1);
        this.oodiClient = oodiClient;
    }

    @Override
    protected SynchronizationItem doEnrich(final SynchronizationItem item) {
        final Course course = item.getCourse();
        if (!StudyRegistryService.isOodiId(course.realisationId)) {
            return item;
        }
        final Optional<BaseOodiCourseUnitRealisation> oodiCourse = oodiClient.getCourseUsers(course.realisationId);

        if (!oodiCourse.isPresent()) {
            return item.completeEnrichmentPhase(
                EnrichmentStatus.ERROR,
                String.format("Course not found from Oodi with id %s", course.realisationId));
        } else if (oodiCourse.map(c -> c.removed).orElse(false)) {
            return item.completeEnrichmentPhase(
                EnrichmentStatus.COURSE_NOT_PUBLIC,
                String.format("Course with id %s removed from Oodi", course.realisationId));
        } else if (oodiCourse.map(this::isCourseEnded).orElse(false)) {
            return item.completeEnrichmentPhase(
                EnrichmentStatus.COURSE_ENDED,
                String.format("Course with realisation id %s has ended", course.realisationId));
        } else {
            return item.setStudyRegistryCourse(Optional.of(oodiCourse.get().toStudyRegistryCourseUnitRealisation()));
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    private boolean isCourseEnded(BaseOodiCourseUnitRealisation oodiCourse) {
        if (oodiCourse.endDate == null) {
            return false;
        }

        LocalDateTime endDatePlusOneYear = LocalDateTime.parse(oodiCourse.endDate, DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT)).plusYears(1);
        LocalDateTime nowDate = LocalDateTime.now();
        return endDatePlusOneYear.isBefore(nowDate);
    }
}
