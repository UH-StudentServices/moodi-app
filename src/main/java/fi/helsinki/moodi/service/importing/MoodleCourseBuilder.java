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

package fi.helsinki.moodi.service.importing;

import fi.helsinki.moodi.integration.moodle.MoodleCourse;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.service.util.MapperService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MoodleCourseBuilder {

    public static final String MOODLE_COURSE_ID_OODI_PREFIX = "oodi_";
    public static final String MOODLE_COURSE_ID_SISU_PREFIX = "sisu_";
    public static final int DEFAULT_NUMBER_OF_SECTIONS = 7;

    @Value("${test.MoodleCourseBuilder.courseVisibility:false}")
    private boolean courseVisibility = false;

    private final MapperService mapperService;

    @Autowired
    public MoodleCourseBuilder(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    private String getShortName(String realisationName, String realisationId) {
        return StringUtils.substring(realisationName, 0, 8) + " " + realisationId;
    }

    public MoodleCourse buildMoodleCourse(StudyRegistryCourseUnitRealisation cur) {
        return new MoodleCourse(
            (cur.origin == StudyRegistryCourseUnitRealisation.Origin.OODI ? MOODLE_COURSE_ID_OODI_PREFIX : MOODLE_COURSE_ID_SISU_PREFIX) + cur.realisationId,
            cur.realisationName,
            getShortName(cur.realisationName, cur.realisationId),
            mapperService.getDefaultCategory(),
            cur.description,
            courseVisibility,
            DEFAULT_NUMBER_OF_SECTIONS,
            cur.startDate,
            cur.endDate
        );
    }
}
