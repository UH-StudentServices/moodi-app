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

import java.util.Optional;

@Service
public class StudyRegistryService {
    public static final String SISU_OODI_COURSE_PREFIX = "hy-CUR-";
    private final SisuClient sisuClient;

    @Autowired
    public StudyRegistryService(SisuClient sisuClient) {
        this.sisuClient = sisuClient;
    }

    public Optional<StudyRegistryCourseUnitRealisation> getCourseUnitRealisation(final String realisationId) {
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

    public static boolean isOodiId(final String realisationId) {
        return realisationId != null && realisationId.matches("\\d+");
    }
}
