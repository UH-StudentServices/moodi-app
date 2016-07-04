package fi.helsinki.moodi.service.importing;

import fi.helsinki.moodi.Constants;
import fi.helsinki.moodi.exception.MissingOrganisationException;
import fi.helsinki.moodi.integration.moodle.MoodleCourse;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiLocalizedValue;
import fi.helsinki.moodi.service.util.MapperService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MoodleCourseBuilder {

    public static final String MOODLE_COURSE_ID_PREFIX = "oodi_";

    private final MapperService mapperService;

    @Autowired
    public MoodleCourseBuilder(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    public MoodleCourse buildMoodleCourse(OodiCourseUnitRealisation oodiCourseUnitRealisation) {

        String preferredLanguage = getPreferredLanguage(oodiCourseUnitRealisation);
        String realisationName = getTranslation(oodiCourseUnitRealisation.realisationName, preferredLanguage);
        String shortName = getShortName(realisationName, oodiCourseUnitRealisation.realisationId);
        String organization = getOrganisation(oodiCourseUnitRealisation);
        String description =  getDescription(oodiCourseUnitRealisation, preferredLanguage);

        return new MoodleCourse(
            MOODLE_COURSE_ID_PREFIX + String.valueOf(oodiCourseUnitRealisation.realisationId),
            realisationName,
            shortName,
            organization,
            description,
            "topics",
            true,
            false,
            false,
            20971520, // 20 MB
            5,
            7
        );
    }

    private String getDescription(OodiCourseUnitRealisation oodiCourseUnitRealisation, String language) {
        return oodiCourseUnitRealisation.descriptions.stream()
            .map(d -> getTranslation(d.texts, language))
            .collect(Collectors.joining(" "));
    }

    private String getShortName(String realisationName, int realisationId) {
        return StringUtils.substring(realisationName, 0, 8) + " " + realisationId;
    }

    private String getOrganisation(OodiCourseUnitRealisation cur) {
        return cur.organisations.stream()
            .sorted((a, b) -> ObjectUtils.compare(a.percentage, b.percentage))
            .findFirst()
            .map(o -> mapperService.getMoodleCategory(o.code))
            .orElseThrow(() -> new MissingOrganisationException(
                "Course realisation with realisationId " + cur.realisationId + " has no organisation"));
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
