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
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiDescription;
import fi.helsinki.moodi.integration.oodi.OodiLanguage;
import fi.helsinki.moodi.integration.oodi.OodiLocale;
import fi.helsinki.moodi.integration.oodi.OodiLocalizedValue;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuDateRange;
import fi.helsinki.moodi.integration.sisu.SisuLearningEnvironment;
import fi.helsinki.moodi.integration.sisu.SisuLocalisedValue;
import fi.helsinki.moodi.integration.sisu.SisuOrganisation;
import fi.helsinki.moodi.integration.sisu.SisuOrganisationRoleShare;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MoodleCourseBuilderTest extends AbstractMoodiIntegrationTest {

    private static final String REALISATION_NAME_FI = "Kurssin nimi";
    private static final String REALISATION_NAME_SV = "Kurs namn";
    private static final String REALISATION_NAME_EN = "Course name";
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
    private static final String MOODLE_DEFAULT_CATEGORY_ID = "17";

    @Autowired
    private MoodleCourseBuilder moodleCourseBuilder;

    @Test
    public void thatItCanBuildMoodleCourse() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_FI,
            LANG_SV,
            LANG_EN);

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(REALISATION_NAME_FI, moodleCourse.fullName);
        assertEquals(MoodleCourseBuilder.MOODLE_COURSE_ID_OODI_PREFIX + REALISATION_ID, moodleCourse.idNumber);
        assertEquals(MOODLE_DEFAULT_CATEGORY_ID, moodleCourse.categoryId);
        assertEquals("Kurssin nimi-1", moodleCourse.shortName);
        assertEquals(String.join(" ", DESCRIPTION_1_FI, DESCRIPTION_2_FI, DESCRIPTION_3_FI), moodleCourse.summary);
    }

    @Test
    public void thatItCanBuildMoodleCourseFromSisuCur() {
        SisuCourseUnitRealisation cur = getSisuCur();
        expectSisuOrganisationExportRequest();

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(REALISATION_NAME_SV, moodleCourse.fullName);
        assertEquals("sisu_hy-cur-1", moodleCourse.idNumber);
        assertEquals("Kurs namn-1", moodleCourse.shortName);
        assertEquals("korrekt url på svenska", moodleCourse.summary);
        assertEquals(LocalDate.of(2019, 8, 5), moodleCourse.startTime);
        assertEquals(LocalDate.of(2019, 12, 5), moodleCourse.endTime);
        assertEquals("9", moodleCourse.categoryId);
    }

    @Test
    public void thatShortNameIsShortAndHasNoSpecialCharacters() {
        SisuCourseUnitRealisation cur = getSisuCur();
        cur.id = "hy-opt-cur-2021-e34bb357-2f08-4a15-a652-6034b9988be2";
        cur.name.sv = "Cöntémporary European History";
        expectSisuOrganisationExportRequest();

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 1000000L);
        assertEquals("Contemporary Europea-LFLS", moodleCourse.shortName);
        assertEquals(MoodleCourseBuilder.MAX_SHORTNAME_LENGTH, moodleCourse.shortName.length());
    }

    @Test
    public void thatShortNamesDoNotCollide() {
        SisuCourseUnitRealisation cur1 = getSisuCur();
        cur1.id = "hy-opt-cur-2021-3129be1f-6110-4b2f-b2e2-9251fe44007b";
        cur1.name.sv = "Oikeuslääketiede syksy L5, C+D 3 op";

        SisuCourseUnitRealisation cur2 = getSisuCur();
        cur2.id = "hy-opt-cur-2122-3129be1f-6110-4b2f-b2e2-9251fe44007b";
        cur2.name.sv = "Oikeuslääketiede, ryhmä C+D, Luento-opetus 3 op";

        expectSisuOrganisationExportRequest();

        MoodleCourse moodleCourse1 = moodleCourseBuilder.buildMoodleCourse(cur1.toStudyRegistryCourseUnitRealisation(), 1L);
        MoodleCourse moodleCourse2 = moodleCourseBuilder.buildMoodleCourse(cur2.toStudyRegistryCourseUnitRealisation(), 2L);

        assertNotEquals(moodleCourse1.shortName, moodleCourse2.shortName);
    }

    @Test
    public void thatItCanBuildMoodleCourseFromSisuCurUsingFallbacks() {
        SisuCourseUnitRealisation cur = getSisuCur();
        cur.teachingLanguageUrn = "urn:code:language:no";
        cur.activityPeriod = null;
        cur.organisations = new ArrayList<>();

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 10000000L);

        assertEquals(REALISATION_NAME_FI, moodleCourse.fullName);
        assertEquals("sisu_hy-cur-1", moodleCourse.idNumber);
        assertEquals("Kurssin nimi-5YC1S", moodleCourse.shortName);
        assertEquals("urli suomeksi", moodleCourse.summary);
        assertEquals(LocalDate.now(), moodleCourse.startTime);
        assertEquals(LocalDate.now().plusYears(1), moodleCourse.endTime);
        assertEquals(MOODLE_DEFAULT_CATEGORY_ID, moodleCourse.categoryId);
    }

    private SisuCourseUnitRealisation getSisuCur() {
        SisuCourseUnitRealisation ret = new SisuCourseUnitRealisation();
        ret.id = "hy-cur-1";
        ret.name = new SisuLocalisedValue(REALISATION_NAME_FI, REALISATION_NAME_SV, REALISATION_NAME_EN);
        ret.activityPeriod = new SisuDateRange(LocalDate.of(2019, 8, 5), LocalDate.of(2019, 11, 5));
        ret.flowState = "PUBLISHED";
        ret.teachingLanguageUrn = "urn:code:language:sv";
        ret.learningEnvironments.add(new SisuLearningEnvironment("urli suomeksi", "fi", true));
        ret.learningEnvironments.add(new SisuLearningEnvironment("fel url på svenska", "sv", false));
        ret.learningEnvironments.add(new SisuLearningEnvironment("korrekt url på svenska", "sv", true));
        ret.organisations = Arrays.asList(
                // Poliittinen historia, not responsible org -> not main org
                new SisuOrganisationRoleShare("urn:code:organisation-role:coordinating-organisation", 1, new SisuOrganisation("hy-org-1000003039")),
                // Fysikaalisen kemian laboratorio, responsible org with largest share -> Is main org = Matlu=Moodle category 9
                new SisuOrganisationRoleShare(RESPONSIBLE_ORGANISATION, 0.6, new SisuOrganisation("hy-org-1000002996")),
                // Teologian ja uskonnontutkimuksen kandiohjelma, responsible with small share -> -> not main org
                new SisuOrganisationRoleShare(RESPONSIBLE_ORGANISATION, 0.4, new SisuOrganisation("hy-org-116716365"))
        );

        return ret;
    }

    @Test
    public void thatItCanBuildMoodleCourseInSwedish() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_SV,
            LANG_FI,
            LANG_EN);

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(REALISATION_NAME_SV, moodleCourse.fullName);
        assertEquals(String.join(" ", DESCRIPTION_1_SV, DESCRIPTION_2_SV, DESCRIPTION_3_SV), moodleCourse.summary);
    }

    @Test
    public void thatItCanBuildMoodleCourseInEnglish() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_EN,
            LANG_SV,
            LANG_FI);

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(REALISATION_NAME_EN, moodleCourse.fullName);
        assertEquals(String.join(" ", DESCRIPTION_1_EN, DESCRIPTION_2_EN, DESCRIPTION_3_EN), moodleCourse.summary);
    }

    @Test
    public void thatItUsesOodiDates() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_EN,
            LANG_SV,
            LANG_FI);
        oodiCourseUnitRealisation.startDate = "2019-08-04T21:00:00.000Z";
        oodiCourseUnitRealisation.endDate = "2019-08-04T21:00:00.000Z";

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(LocalDate.of(2019, 8, 5), moodleCourse.startTime);
        assertEquals(LocalDate.of(2019, 9, 5), moodleCourse.endTime);
    }

    @Test
    public void thatItUsesDefaultsIfOodiDatesAreMissing() {
        OodiCourseUnitRealisation oodiCourseUnitRealisation = getOodiCourseUnitRealisation(
            LANG_EN,
            LANG_SV,
            LANG_FI);
        oodiCourseUnitRealisation.startDate = "";
        oodiCourseUnitRealisation.endDate = null;

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(oodiCourseUnitRealisation.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(LocalDate.now(), moodleCourse.startTime);
        assertEquals(LocalDate.now().plusYears(1), moodleCourse.endTime);
    }

    private OodiCourseUnitRealisation getOodiCourseUnitRealisation(String... langcodes) {
        List<OodiLanguage> languages = Arrays.stream(langcodes)
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

}
