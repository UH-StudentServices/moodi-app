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
import fi.helsinki.moodi.integration.oodi.*;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.Constants.*;
import static org.junit.Assert.assertEquals;

public class MoodleCourseBuilderTest extends AbstractMoodiIntegrationTest {

    private static final String REALISATION_NAME_FI = "Course realisation name (fi)";
    private static final String REALISATION_NAME_SV = "Course realisation name (sv)";
    private static final String REALISATION_NAME_EN = "Course realisation name (en)";
    private static final int REALISATION_ID = 1;

    private static final String DESCRIPTION_1_FI = "Course description 1 (fi)";
    private static final String DESCRIPTION_2_FI = "Course description 2 (fi)";
    private static final String DESCRIPTION_3_FI = "Course description 3 (fi)";
    private static final String DESCRIPTION_1_SV = "Course description 1 (sv)";
    private static final String DESCRIPTION_2_SV = "Course description 2 (sv)";
    private static final String DESCRIPTION_3_SV = "Course description 3 (sv)";
    private static final String DESCRIPTION_1_EN = "Course description 1 (en)";
    private static final String DESCRIPTION_2_EN = "Course description 2 (en)";
    private static final String DESCRIPTION_3_EN = "Course description 3 (en)";
    private static final Integer DESCRIPTION_1_ID = 10;
    private static final Integer DESCRIPTION_2_ID = 20;
    private static final Integer DESCRIPTION_3_ID = 30;
    private static final String MOODLE_CATEGORY_ID = "73";

    @Autowired
    private MoodleCourseBuilder moodleCourseBuilder;

    @Test
    public void thatItCanBuildMoodleCourse() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_FI,
            LANG_SV,
            LANG_EN);

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation);

        assertEquals(REALISATION_NAME_FI, moodleCourse.fullName);
        assertEquals(MoodleCourseBuilder.MOODLE_COURSE_ID_PREFIX + REALISATION_ID, moodleCourse.idnumber);
        assertEquals(MOODLE_CATEGORY_ID, moodleCourse.categoryId);
        assertEquals("Course r " + REALISATION_ID, moodleCourse.shortName);
        assertEquals(String.join(" ", DESCRIPTION_1_FI, DESCRIPTION_2_FI, DESCRIPTION_3_FI), moodleCourse.summary);
    }

    @Test
    public void thatItCanBuildMoodleCourseInSwedish() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_SV,
            LANG_FI,
            LANG_EN);

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation);

        assertEquals(REALISATION_NAME_SV, moodleCourse.fullName);
        assertEquals(String.join(" ", DESCRIPTION_1_SV, DESCRIPTION_2_SV, DESCRIPTION_3_SV), moodleCourse.summary);
    }

    @Test
    public void thatItCanBuildMoodleCourseInEnglish() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_EN,
            LANG_SV,
            LANG_FI);

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation);

        assertEquals(REALISATION_NAME_EN, moodleCourse.fullName);
        assertEquals(String.join(" ", DESCRIPTION_1_EN, DESCRIPTION_2_EN, DESCRIPTION_3_EN), moodleCourse.summary);
    }

    @Test
    public void testGetFirstSupportedMoodleLanguageOrDefaultWithOnlySupportedLangs() {
        testGetFirstSupportedMoodleLanguageOrDefault(LANG_FI, LANG_FI, LANG_EN, LANG_SV);
    }

    @Test
    public void testGetFirstSupportedMoodleLanguageOrDefaultWithSomeUnsupportedLangs() {
        testGetFirstSupportedMoodleLanguageOrDefault(LANG_FI, "ru", "es", LANG_FI, LANG_SV);
    }

    @Test
    public void testGetFirstSupportedMoodleLanguageOrDefaultWithOnlyUnsupportedLangs() {
        testGetFirstSupportedMoodleLanguageOrDefault(LANG_EN, "ru", "es");
    }

    @Test
    public void testGetFirstSupportedMoodleLanguageOrDefaultWithoutCourseLangs() {
        testGetFirstSupportedMoodleLanguageOrDefault(LANG_FI);
    }


    private void testGetFirstSupportedMoodleLanguageOrDefault(String expectedLang, String... courseLangs) {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(courseLangs);

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation);

        assertEquals(expectedLang, moodleCourse.langCode);
    }


    private OodiCourseUnitRealisation getOodiCourseUnitRealisation(String... langcodes) {
        List<OodiLanguage> languages = Arrays.asList(langcodes).stream()
            .map(this::getOodiLanguage)
            .collect(Collectors.toList());

        OodiCourseUnitRealisation oodiCourseUnitRealisation = new OodiCourseUnitRealisation();
        oodiCourseUnitRealisation.realisationId = REALISATION_ID;
        oodiCourseUnitRealisation.languages = languages;
        oodiCourseUnitRealisation.realisationName = newArrayList(
            getOodiLocalizedValue(REALISATION_NAME_FI, LANG_FI),
            getOodiLocalizedValue(REALISATION_NAME_SV, LANG_SV),
            getOodiLocalizedValue(REALISATION_NAME_EN, LANG_EN)
        );
        oodiCourseUnitRealisation.descriptions = getOodiDescriptions();
        return oodiCourseUnitRealisation;
    }

    private OodiLanguage getOodiLanguage(String langcode) {
        OodiLanguage oodiLanguage = new OodiLanguage();
        oodiLanguage.langCode = langcode;
        return oodiLanguage;
    }

    private List<OodiDescription> getOodiDescriptions() {
        return newArrayList(
            getOodiDescription(DESCRIPTION_1_ID, newArrayList(
                getOodiLocalizedValue(DESCRIPTION_1_FI, LANG_FI),
                getOodiLocalizedValue(DESCRIPTION_1_SV, LANG_SV),
                getOodiLocalizedValue(DESCRIPTION_1_EN, LANG_EN)
                )),
            getOodiDescription(DESCRIPTION_2_ID, newArrayList(
                getOodiLocalizedValue(DESCRIPTION_2_FI, LANG_FI),
                getOodiLocalizedValue(DESCRIPTION_2_SV, LANG_SV),
                getOodiLocalizedValue(DESCRIPTION_2_EN, LANG_EN)
            )),
            getOodiDescription(DESCRIPTION_3_ID, newArrayList(
                getOodiLocalizedValue(DESCRIPTION_3_FI, LANG_FI),
                getOodiLocalizedValue(DESCRIPTION_3_SV, LANG_SV),
                getOodiLocalizedValue(DESCRIPTION_3_EN, LANG_EN)
            ))
        );
    }

    private OodiDescription getOodiDescription(Integer id, List<OodiLocalizedValue> oodiLocalizedValues) {
        OodiDescription oodiDescription = new OodiDescription();
        oodiDescription.id = id;
        oodiDescription.texts = oodiLocalizedValues;
        return oodiDescription;
    }

    private OodiLocalizedValue getOodiLocalizedValue(String value, String langCode) {
        OodiLocalizedValue oodiLocalizedValue = new OodiLocalizedValue();
        oodiLocalizedValue.langcode = OodiLocale.valueOf(langCode.toUpperCase());
        oodiLocalizedValue.text = value;
        return oodiLocalizedValue;
    }

    private OodiOrganisation getOodiOrganisation(int percentage, String code) {
        OodiOrganisation oodiOrganisation = new OodiOrganisation();
        oodiOrganisation.percentage = percentage;
        oodiOrganisation.code = code;
        return oodiOrganisation;
    }

}
