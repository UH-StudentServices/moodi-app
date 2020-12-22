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

package fi.helsinki.moodi.integration.studyregistry;

import fi.helsinki.moodi.integration.oodi.OodiClient;
import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.partitioningBy;

@Service
public class StudyRegistryService {

    private final OodiClient oodiClient;
    private final SisuClient sisuClient;

    @Autowired
    public StudyRegistryService(OodiClient oodiClient, SisuClient sisuClient) {
        this.oodiClient = oodiClient;
        this.sisuClient = sisuClient;
    }

    public Optional<StudyRegistryCourseUnitRealisation> getCourseUnitRealisation(final String realisationId) {
        if (isOodiId(realisationId)) {
            return oodiClient.getCourseUnitRealisation(realisationId).flatMap(cur -> Optional.of(cur.toStudyRegistryCourseUnitRealisation()));
        } else {
            SisuCourseUnitRealisation sisuCur = sisuClient.getCourseUnitRealisation(realisationId);
            if (sisuCur != null) {
                StudyRegistryCourseUnitRealisation cur = sisuCur.toStudyRegistryCourseUnitRealisation();
                // Diverging from existing pattern here:
                // Getting all teacher user names immediately, as opposed to getting them one by one in EnrollmentExecutor,
                // because it is simpler and more efficient this way.
                cur.teachers = SisuPerson.toStudyRegistryTeachers(sisuClient.getPersons(sisuCur.teacherSisuIds()));
                return Optional.of(cur);
            } else {
                return Optional.empty();
            }
        }
    }

    public List<StudyRegistryCourseUnitRealisation> getCourseUnitRealisations(final List<String> realisationIds) {
        Map<Boolean, List<String>> idsByIsOodiId = realisationIds.stream().collect(partitioningBy(StudyRegistryService::isOodiId));

        List<SisuCourseUnitRealisation> sisuCurs = sisuClient.getCourseUnitRealisations(idsByIsOodiId.get(false));

        List<String> uniquePersonIds = sisuCurs.stream().flatMap(cur -> cur.teacherSisuIds().stream()).distinct().collect(Collectors.toList());

        Map<String, StudyRegistryTeacher> teachersById =
            sisuClient.getPersons(uniquePersonIds)
                .stream().collect(Collectors.toMap(p -> p.id, p -> p.toStudyRegistryTeacher()));

        Stream<StudyRegistryCourseUnitRealisation> sisu = sisuCurs.stream()
            .map(cur -> cur.toStudyRegistryCourseUnitRealisation(teachersById));

        Stream<StudyRegistryCourseUnitRealisation> oodi =
            idsByIsOodiId.get(true).stream().map(id -> getCourseUnitRealisation(id)).filter(o -> o.isPresent()).map(o -> o.get());

        return Stream.concat(sisu, oodi).collect(Collectors.toList());
    }

    public static boolean isOodiId(final String realisationId) {
        return realisationId != null && realisationId.matches("\\d+");
    }
}
