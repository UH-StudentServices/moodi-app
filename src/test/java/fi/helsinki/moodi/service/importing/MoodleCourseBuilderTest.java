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
import fi.helsinki.moodi.integration.sisu.SisuCourseUnit;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisationType;
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
    public void thatItCanBuildMoodleCourseFromSisuCur() {
        SisuCourseUnitRealisation cur = getSisuCur("sv");
        expectSisuOrganisationExportRequest();

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(generateMultiLangSpan("sv", REALISATION_NAME_SV)
                + generateMultiLangSpan("fi", REALISATION_NAME_FI)
                + generateMultiLangSpan("en", REALISATION_NAME_EN), moodleCourse.fullName);
        assertEquals("hy-cur-1", moodleCourse.idNumber);
        assertEquals("Kurs namn-1", moodleCourse.shortName);
        assertEquals("<p><span lang=\"fi\" class=\"multilang\"><a href=\"urli suomeksi\">urli suomeksi</a></span>" +
            "<span lang=\"en\" class=\"multilang\"><a href=\"urli suomeksi\">urli suomeksi</a></span>" +
            "<span lang=\"sv\" class=\"multilang\"><a href=\"fel url på svenska\">fel url på svenska</a></span></p>" +
            "<p><span lang=\"fi\" class=\"multilang\">Opintojaksot</span><span lang=\"en\" class=\"multilang\">Courses</span>" +
            "<span lang=\"sv\" class=\"multilang\">Studieavsnitten</span> CODE1, CODE2, CODE3</p><p>" +
            "<span lang=\"fi\" class=\"multilang\">Kurssi</span><span lang=\"en\" class=\"multilang\">Course</span>" +
            "<span lang=\"sv\" class=\"multilang\">Kurs</span>, 5.8.2019–4.11.2019</p>", moodleCourse.summary);
        assertEquals(LocalDate.of(2019, 8, 5), moodleCourse.startTime);
        assertEquals(LocalDate.of(2019, 12, 5), moodleCourse.endTime);
        assertEquals("9", moodleCourse.categoryId);
    }

    @Test
    public void thatItCanBuildMoodleCourseFromSisuCurInFinnish() {
        SisuCourseUnitRealisation cur = getSisuCur("fi");
        expectSisuOrganisationExportRequest();

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 1L);

        assertEquals(generateMultiLangSpan("fi", REALISATION_NAME_FI)
            + generateMultiLangSpan("sv", REALISATION_NAME_SV)
            + generateMultiLangSpan("en", REALISATION_NAME_EN), moodleCourse.fullName);
        assertEquals("hy-cur-1", moodleCourse.idNumber);
        assertEquals("Kurssin nimi-1", moodleCourse.shortName);
        assertEquals("<p><span lang=\"fi\" class=\"multilang\"><a href=\"urli suomeksi\">urli suomeksi</a></span>" +
            "<span lang=\"en\" class=\"multilang\"><a href=\"urli suomeksi\">urli suomeksi</a></span>" +
            "<span lang=\"sv\" class=\"multilang\"><a href=\"fel url på svenska\">fel url på svenska</a></span></p>" +
            "<p><span lang=\"fi\" class=\"multilang\">Opintojaksot</span><span lang=\"en\" class=\"multilang\">Courses</span>" +
            "<span lang=\"sv\" class=\"multilang\">Studieavsnitten</span> CODE1, CODE2, CODE3</p><p>" +
            "<span lang=\"fi\" class=\"multilang\">Kurssi</span><span lang=\"en\" class=\"multilang\">Course</span>" +
            "<span lang=\"sv\" class=\"multilang\">Kurs</span>, 5.8.2019–4.11.2019</p>", moodleCourse.summary);
        assertEquals(LocalDate.of(2019, 8, 5), moodleCourse.startTime);
        assertEquals(LocalDate.of(2019, 12, 5), moodleCourse.endTime);
        assertEquals("9", moodleCourse.categoryId);
    }

    @Test
    public void thatShortNameIsShortAndHasNoSpecialCharacters() {
        SisuCourseUnitRealisation cur = getSisuCur("sv");
        cur.id = "hy-opt-cur-2021-e34bb357-2f08-4a15-a652-6034b9988be2";
        cur.name.sv = "Cöntémporary European History";
        expectSisuOrganisationExportRequest();

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 1000000L);
        assertEquals("Contemporary Europea-LFLS", moodleCourse.shortName);
        assertEquals(MoodleCourseBuilder.MAX_SHORTNAME_LENGTH, moodleCourse.shortName.length());
    }

    @Test
    public void thatShortNamesDoNotCollide() {
        SisuCourseUnitRealisation cur1 = getSisuCur("sv");
        cur1.id = "hy-opt-cur-2021-3129be1f-6110-4b2f-b2e2-9251fe44007b";
        cur1.name.sv = "Oikeuslääketiede syksy L5, C+D 3 op";

        SisuCourseUnitRealisation cur2 = getSisuCur("sv");
        cur2.id = "hy-opt-cur-2122-3129be1f-6110-4b2f-b2e2-9251fe44007b";
        cur2.name.sv = "Oikeuslääketiede, ryhmä C+D, Luento-opetus 3 op";

        expectSisuOrganisationExportRequest();

        MoodleCourse moodleCourse1 = moodleCourseBuilder.buildMoodleCourse(cur1.toStudyRegistryCourseUnitRealisation(), 1L);
        MoodleCourse moodleCourse2 = moodleCourseBuilder.buildMoodleCourse(cur2.toStudyRegistryCourseUnitRealisation(), 2L);

        assertNotEquals(moodleCourse1.shortName, moodleCourse2.shortName);
    }

    @Test
    public void thatItCanBuildMoodleCourseFromSisuCurUsingFallbacks() {
        SisuCourseUnitRealisation cur = getSisuCur("sv");
        cur.teachingLanguageUrn = "urn:code:language:no";
        cur.activityPeriod = null;
        cur.organisations = new ArrayList<>();

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 10000000L);

        assertEquals(generateMultiLangSpan("fi", REALISATION_NAME_FI)
            + generateMultiLangSpan("sv", REALISATION_NAME_SV)
            + generateMultiLangSpan("en", REALISATION_NAME_EN), moodleCourse.fullName);
        assertEquals("hy-cur-1", moodleCourse.idNumber);
        assertEquals("Kurssin nimi-5YC1S", moodleCourse.shortName);
        assertEquals("<p><span lang=\"fi\" class=\"multilang\"><a href=\"urli suomeksi\">urli suomeksi</a></span>" +
            "<span lang=\"en\" class=\"multilang\"><a href=\"urli suomeksi\">urli suomeksi</a></span>" +
            "<span lang=\"sv\" class=\"multilang\"><a href=\"fel url på svenska\">fel url på svenska</a></span></p>" +
            "<p><span lang=\"fi\" class=\"multilang\">Opintojaksot</span><span lang=\"en\" class=\"multilang\">Courses</span>" +
            "<span lang=\"sv\" class=\"multilang\">Studieavsnitten</span> CODE1, CODE2, CODE3</p><p>" +
            "<span lang=\"fi\" class=\"multilang\">Kurssi</span><span lang=\"en\" class=\"multilang\">Course</span>" +
            "<span lang=\"sv\" class=\"multilang\">Kurs</span></p>", moodleCourse.summary);
        assertEquals(LocalDate.now(), moodleCourse.startTime);
        assertEquals(LocalDate.now().plusYears(1), moodleCourse.endTime);
        assertEquals(MOODLE_DEFAULT_CATEGORY_ID, moodleCourse.categoryId);
    }

    @Test
    public void thatNullOrganisationsReturnDefaultCategory() {
        SisuCourseUnitRealisation cur = getSisuCur("sv");
        cur.teachingLanguageUrn = "urn:code:language:sv";
        cur.organisations = null;

        MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(cur.toStudyRegistryCourseUnitRealisation(), 10000000L);

        assertEquals(MOODLE_DEFAULT_CATEGORY_ID, moodleCourse.categoryId);
    }

    private SisuCourseUnitRealisation getSisuCur(String langCode) {
        SisuCourseUnitRealisation ret = new SisuCourseUnitRealisation();
        ret.id = "hy-cur-1";
        ret.courseUnits.add(new SisuCourseUnit("CODE1"));
        ret.courseUnits.add(new SisuCourseUnit("CODE2"));
        ret.courseUnits.add(new SisuCourseUnit("CODE3"));
        ret.courseUnitRealisationType = new SisuCourseUnitRealisationType();
        ret.courseUnitRealisationType.name = new SisuLocalisedValue(REALISATION_TYPE_FI, REALISATION_TYPE_SV, REALISATION_TYPE_EN);
        ret.name = new SisuLocalisedValue(REALISATION_NAME_FI, REALISATION_NAME_SV, REALISATION_NAME_EN);
        ret.activityPeriod = new SisuDateRange(LocalDate.of(2019, 8, 5), LocalDate.of(2019, 11, 5));
        ret.flowState = "PUBLISHED";
        ret.teachingLanguageUrn = "urn:code:language:" + langCode;
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
}
