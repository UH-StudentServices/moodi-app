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

import fi.helsinki.moodi.Constants;
import fi.helsinki.moodi.integration.moodle.MoodleCourse;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiLocalizedValue;
import fi.helsinki.moodi.service.util.MapperService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.util.DateFormat.OODI_UTC_DATE_FORMAT;

@Component
public class MoodleCourseBuilder {

    public static final String MOODLE_COURSE_ID_PREFIX = "oodi_";
    public static final int DEFAULT_NUMBER_OF_SECTIONS = 7;

    @Value("${test.MoodleCourseBuilder.courseVisibility:false}")
    private boolean courseVisibility = false;

    private final MapperService mapperService;

    @Autowired
    public MoodleCourseBuilder(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    public MoodleCourse buildMoodleCourse(OodiCourseUnitRealisation oodiCourseUnitRealisation) {

        String preferredLanguage = getPreferredLanguage(oodiCourseUnitRealisation);
        String realisationName = getTranslation(oodiCourseUnitRealisation.realisationName, preferredLanguage);
        String shortName = getShortName(realisationName, oodiCourseUnitRealisation.realisationId);
        String moodleCategory = mapperService.getDefaultCategory();
        String description =  getDescription(oodiCourseUnitRealisation, preferredLanguage);
        LocalDateTime endDatePlusOneMonth = parseDateTime(oodiCourseUnitRealisation.endDate, LocalDateTime.now().plusMonths(11)).plusMonths(1);

        return new MoodleCourse(
            MOODLE_COURSE_ID_PREFIX + String.valueOf(oodiCourseUnitRealisation.realisationId),
            realisationName,
            shortName,
            moodleCategory,
            description,
            courseVisibility,
            DEFAULT_NUMBER_OF_SECTIONS,
            parseDateTime(oodiCourseUnitRealisation.startDate, LocalDateTime.now()),
            endDatePlusOneMonth
        );
    }

    private LocalDateTime parseDateTime(String s, LocalDateTime defaultValue) {
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT));
        } catch (Exception e) {
            // Fall through and return defaultValue
        }
        return defaultValue;
    }

    private String getDescription(OodiCourseUnitRealisation oodiCourseUnitRealisation, String language) {
        return oodiCourseUnitRealisation.descriptions.stream()
            .map(d -> getTranslation(d.texts, language))
            .collect(Collectors.joining(" "));
    }

    private String getShortName(String realisationName, int realisationId) {
        return StringUtils.substring(realisationName, 0, 8) + " " + realisationId;
    }

    private String getTranslation(List<OodiLocalizedValue> oodiLocalizedValues, String language) {
        return getTranslationByLanguage(oodiLocalizedValues, language)
            .orElseGet(() -> getFirstTranslation(oodiLocalizedValues)
            .orElse(""));
    }

    private String getPreferredLanguage(OodiCourseUnitRealisation oodiCourseUnitRealisation) {
        return oodiCourseUnitRealisation.languages.stream()
            .findFirst()
            .map(l -> l.langCode)
            .orElse(Constants.LANG_DEFAULT);
    }

    private Optional<String> getTranslationByLanguage(List<OodiLocalizedValue> oodiLocalizedValues, String language) {
        return oodiLocalizedValues.stream()
            .filter(l -> l.langcode.toString().equals(language))
            .findFirst()
            .map(l -> l.text);
    }

    private Optional<String> getFirstTranslation(List<OodiLocalizedValue> oodiLocalizedValues) {
        return oodiLocalizedValues.stream()
            .findFirst()
            .map(l -> l.text);
    }
}
