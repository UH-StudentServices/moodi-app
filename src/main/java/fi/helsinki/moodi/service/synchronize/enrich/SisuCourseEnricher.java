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

import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryService;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SisuCourseEnricher extends AbstractEnricher {
    private static final Logger logger = LoggerFactory.getLogger(SisuCourseEnricher.class);
    private final StudyRegistryService studyRegistryService;
    private Map<String, StudyRegistryCourseUnitRealisation> prefetchedCursById = new HashMap<>();

    @Autowired
    protected SisuCourseEnricher(StudyRegistryService studyRegistryService) {
        super(1);
        this.studyRegistryService = studyRegistryService;
    }

    public void prefetchCourses(List<String> curIds) {
        List<String> uniqueSisuIds = new LinkedHashSet<String>(curIds).stream()
            .filter(id -> !StudyRegistryService.isOodiId(id)).collect(Collectors.toList());
        prefetchedCursById = studyRegistryService.getCourseUnitRealisations(uniqueSisuIds).stream()
            .collect(Collectors.toMap(c -> c.realisationId, c -> c));
    }

    @Override
    protected SynchronizationItem doEnrich(SynchronizationItem item) {
        final Course course = item.getCourse();
        if (StudyRegistryService.isOodiId(course.realisationId)) {
            return item;
        }
        final StudyRegistryCourseUnitRealisation cur = prefetchedCursById.get(course.realisationId);

        if (cur == null) {
            return item.completeEnrichmentPhase(
                EnrichmentStatus.ERROR,
                String.format("Course not found from Sisu with id %s", course.realisationId));
        } else if (!cur.published) {
            return item.completeEnrichmentPhase(
                EnrichmentStatus.COURSE_NOT_PUBLIC,
                String.format("Course with id %s is not public in Sisu", course.realisationId));
        } else if (endedMoreThanYearAgo(cur)) {
            return item.completeEnrichmentPhase(
                EnrichmentStatus.COURSE_ENDED,
                String.format("Course with realisation id %s has ended", course.realisationId));
        } else {
            return item.setStudyRegistryCourse(Optional.of(cur));
        }
    }

    private boolean endedMoreThanYearAgo(StudyRegistryCourseUnitRealisation cur) {
        return cur.endDate.plusYears(1).isBefore(LocalDate.now());
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
