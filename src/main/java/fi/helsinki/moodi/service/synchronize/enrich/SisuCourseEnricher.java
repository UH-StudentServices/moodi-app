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

import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuPerson;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryService;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
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

import static java.util.stream.Collectors.partitioningBy;

@Component
public class SisuCourseEnricher extends AbstractEnricher {
    private static final Logger logger = LoggerFactory.getLogger(SisuCourseEnricher.class);
    private final SisuClient sisuClient;
    private Map<String, StudyRegistryCourseUnitRealisation> prefetchedCursById = new HashMap<>();

    @Autowired
    protected SisuCourseEnricher(SisuClient sisuClient) {
        super(1);
        this.sisuClient = sisuClient;
    }

    public void prefetchCourses(List<String> curIds) {
        List<String> uniqueSisuIds = new LinkedHashSet<>(curIds).stream()
            .filter(id -> !StudyRegistryService.isOodiId(id)).collect(Collectors.toList());
        prefetchedCursById = getSisuCourseUnitRealisations(uniqueSisuIds).stream()
            .collect(Collectors.toMap(c -> c.realisationId, c -> c));
    }

    private List<StudyRegistryCourseUnitRealisation> getSisuCourseUnitRealisations(final List<String> realisationIds) {
        Map<Boolean, List<String>> idsByIsOodiId = realisationIds.stream().collect(partitioningBy(StudyRegistryService::isOodiId));

        List<SisuCourseUnitRealisation> sisuCurs = sisuClient.getCourseUnitRealisations(idsByIsOodiId.get(false));

        List<String> uniquePersonIds = sisuCurs.stream().flatMap(cur -> cur.teacherSisuIds().stream()).distinct().collect(Collectors.toList());

        Map<String, StudyRegistryTeacher> teachersById =
            sisuClient.getPersons(uniquePersonIds)
                .stream().collect(Collectors.toMap(p -> p.id, SisuPerson::toStudyRegistryTeacher));

        return sisuCurs.stream()
            .map(cur -> cur.toStudyRegistryCourseUnitRealisation(teachersById)).collect(Collectors.toList());
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
