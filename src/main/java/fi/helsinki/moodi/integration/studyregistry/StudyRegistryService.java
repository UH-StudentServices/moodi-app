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

import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.partitioningBy;

@Service
public class StudyRegistryService {
    public static final String SISU_OODI_COURSE_PREFIX = "hy-CUR-";
    private final SisuClient sisuClient;

    @Autowired
    public StudyRegistryService(SisuClient sisuClient) {
        this.sisuClient = sisuClient;
    }

    public Optional<StudyRegistryCourseUnitRealisation> getSisuCourseUnitRealisation(final String realisationId) {
        return getSisuCourseUnitRealisations(Arrays.asList(realisationId)).stream().findFirst();
    }

    public List<StudyRegistryCourseUnitRealisation> getSisuCourseUnitRealisations(final List<String> realisationIds) {
        Map<Boolean, List<String>> idsByIsOodiId = realisationIds.stream().collect(partitioningBy(StudyRegistryService::isOodiId));

        List<SisuCourseUnitRealisation> sisuCurs = sisuClient.getCourseUnitRealisations(idsByIsOodiId.get(false));

        List<String> uniquePersonIds = sisuCurs.stream().flatMap(cur -> cur.teacherSisuIds().stream()).distinct().collect(Collectors.toList());

        // Diverging from existing pattern here:
        // Getting all teacher user names immediately, as opposed to getting them one by one in EnrollmentExecutor,
        // because it is simpler and more efficient this way.
        Map<String, StudyRegistryTeacher> teachersById =
            sisuClient.getPersons(uniquePersonIds)
                .stream().collect(Collectors.toMap(p -> p.id, SisuPerson::toStudyRegistryTeacher));

        return sisuCurs.stream()
            .map(cur -> cur.toStudyRegistryCourseUnitRealisation(teachersById)).collect(Collectors.toList());
    }

    public static boolean isOodiId(final String realisationId) {
        return realisationId != null && realisationId.matches("\\d+");
    }
}
