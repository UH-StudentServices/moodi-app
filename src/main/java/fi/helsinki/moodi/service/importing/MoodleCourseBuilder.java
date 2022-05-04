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

import java.text.Normalizer;

@Component
public class MoodleCourseBuilder {

    public static final int DEFAULT_NUMBER_OF_SECTIONS = 7;
    public static final int MAX_SHORTNAME_LENGTH = 25;

    @Value("${test.MoodleCourseBuilder.courseVisibility:false}")
    private boolean courseVisibility = false;

    @Value("${test.MoodleCourseBuilder.overrideShortname:false}")
    private boolean overrideShortname = false;

    private final MapperService mapperService;

    @Autowired
    public MoodleCourseBuilder(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    private String getShortName(String realisationName, Long dbCourseId) {
        if (overrideShortname) {
            // DB IDs in integration tests are thzxe same for each run, so we must override them to something unique
            // to avoid shortName collisions in dev Moodle.
            dbCourseId = System.currentTimeMillis();
        }
        String uniqueSuffix = "-" + Long.toString(dbCourseId, Character.MAX_RADIX).toUpperCase();
        // Transform to ASCII.
        String cleanName = Normalizer.normalize(realisationName, Normalizer.Form.NFKD).replaceAll("[^\\x00-\\x7F]", "");
        // Adjust name length according to unique identifier length.
        return StringUtils.substring(cleanName, 0, MAX_SHORTNAME_LENGTH - uniqueSuffix.length())
                + uniqueSuffix;
    }

    public MoodleCourse buildMoodleCourse(StudyRegistryCourseUnitRealisation cur, Long dbCourseId) {
        return new MoodleCourse(cur.realisationId,
            Normalizer.normalize(cur.realisationName, Normalizer.Form.NFKD).replaceAll("[^\\x00-\\x7F]", ""),
            getShortName(cur.teachingLanguageRealisationName, dbCourseId),
            mapperService.getMoodleCategoryByOrganisationId(cur.mainOrganisationId),
            cur.description,
            courseVisibility,
            DEFAULT_NUMBER_OF_SECTIONS,
            cur.startDate,
            cur.endDate
        );
    }
}
