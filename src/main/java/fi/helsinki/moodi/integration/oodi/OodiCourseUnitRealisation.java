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

package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.helsinki.moodi.Constants;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

public class OodiCourseUnitRealisation extends BaseOodiCourseUnitRealisation {

    @JsonProperty("languages")
    public List<OodiLanguage> languages = newArrayList();

    @JsonProperty("descriptions")
    public List<OodiDescription> descriptions = newArrayList();

    @JsonProperty("realisation_name")
    public List<OodiLocalizedValue> realisationName = newArrayList();

    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation() {
        StudyRegistryCourseUnitRealisation ret = super.toStudyRegistryCourseUnitRealisation();

        String preferredLanguage = getPreferredLanguage();
        ret.realisationName = getTranslation(this.realisationName, preferredLanguage);
        ret.description =  getDescription(preferredLanguage);

        return ret;
    }

    private String getDescription(String language) {
        return descriptions.stream()
            .map(d -> getTranslation(d.texts, language))
            .collect(Collectors.joining(" "));
    }

    private String getTranslation(List<OodiLocalizedValue> oodiLocalizedValues, String language) {
        return getTranslationByLanguage(oodiLocalizedValues, language)
            .orElseGet(() -> getFirstTranslation(oodiLocalizedValues)
                .orElse(""));
    }

    private String getPreferredLanguage() {
        return languages.stream()
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
