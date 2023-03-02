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

package fi.helsinki.moodi.integration.sisu;

import org.junit.Before;
import org.junit.Test;
import static fi.helsinki.moodi.integration.sisu.SisuLocale.FI;
import static fi.helsinki.moodi.integration.sisu.SisuLocale.SV;
import static fi.helsinki.moodi.integration.sisu.SisuLocale.EN;
import static org.junit.Assert.assertEquals;

public class SisuCourseUnitRealisationTest {

    public static final String CUR_ID_OPTIME = "hy-OPT-CUR-1234";
    public static final String CUR_ID_OODI = "hy-CUR-12345";
    public static final String CUR_ID_SISU = "otm-1234";
    public static final String NAME_FI = "suomi";
    public static final String NAME_SV = "ruotsi";
    public static final String NAME_EN = "englanti";

    public static final String SPECIFIER_FI = "1";
    public static final String SPECIFIER_SV = "2";
    public static final String SPECIFIER_EN = "3";
    public static final String SPAN_START = "<span lang=\":lang:\" class=\"multilang\">";
    public static final String SPAN_END = "</span>";
    public SisuCourseUnitRealisation cur;

    @Before
    public void setUp() {
        cur = new SisuCourseUnitRealisation();
        cur.id = CUR_ID_SISU;
        cur.name = new SisuLocalisedValue(NAME_FI, NAME_SV, NAME_EN);
        cur.nameSpecifier = new SisuLocalisedValue(SPECIFIER_FI, SPECIFIER_SV, SPECIFIER_EN);
    }

    @Test
    public void testGetForLocaleOrFirstAvailableReturnsStringIfAvailable() {
        cur.name.fi = null;
        cur.name.en = null;
        assertEquals(NAME_SV, cur.name.getForLocaleOrFirstAvailable(EN));
    }

    @Test
    public void testGetForLocaleOrFirstAvailableReturnsCorrectLocale() {
        assertEquals(NAME_EN, cur.name.getForLocaleOrFirstAvailable(EN));
    }

    @Test
    public void testGetForLocaleOrFirstAvailableReturnsEmptyStringIfNotFound() {
        cur.name.fi = null;
        cur.name.en = null;
        cur.name.sv = null;
        assertEquals("", cur.name.getForLocaleOrFirstAvailable(EN));
    }

    @Test
    public void testNameLocalizationAllLanguages() {
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, cur.calculateName(FI))
            + generateSpan(SV, cur.calculateName(SV))
            + generateSpan(EN, cur.calculateName(EN)), name);
    }

    @Test
    public void testNameLocalizationTwoLanguages() {
        cur.name.sv = null;
        cur.nameSpecifier.sv = null;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, cur.calculateName(FI)) + generateSpan(EN, cur.calculateName(EN)), name);
    }

    @Test
    public void testNameLocalizationTwoLanguagesOtherParams() {
        cur.name.fi = null;
        cur.nameSpecifier.fi = null;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(SV, cur.calculateName(SV)) + generateSpan(EN, cur.calculateName(EN)), name);
    }

    @Test
    public void testNameLocalizationOnlyTeachingLanguage() {
        cur.name.sv = null;
        cur.nameSpecifier.sv = null;
        cur.name.en = null;
        cur.nameSpecifier.en = null;
        String name = cur.generateName(FI);
        assertEquals(cur.calculateName(FI), name);
    }

    @Test
    public void testNameLocalizationAllIdentical() {
        cur.name.sv = NAME_FI;
        cur.nameSpecifier.sv = SPECIFIER_FI;
        cur.name.en = NAME_FI;
        cur.nameSpecifier.en = SPECIFIER_FI;
        String name = cur.generateName(FI);
        assertEquals(cur.calculateName(FI), name);
    }

    @Test
    public void testOneLocalizationIsIdenticalToDefault() {
        cur.name.sv = NAME_FI;
        cur.nameSpecifier.sv = SPECIFIER_FI;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, cur.calculateName(FI))
            + generateSpan(EN, cur.calculateName(EN)), name);
    }

    @Test
    public void testDefaultLocalizationMissingButThereAreTwoOtherOptions() {
        cur.name.fi = null;
        cur.nameSpecifier.fi = null;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(SV, cur.calculateName(SV))
            + generateSpan(EN, cur.calculateName(EN)), name);
    }

    @Test
    public void testDefaultLocalizationMissingButThereIsAnotherOption() {
        cur.name.fi = null;
        cur.nameSpecifier.fi = null;
        cur.name.sv = null;
        cur.nameSpecifier.sv = null;
        String name = cur.generateName(FI);
        assertEquals(cur.calculateName(EN), name);
    }

    @Test
    public void testNameTooLong() {
        cur.name.sv = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        String name = cur.generateName(EN);
        assertEquals(cur.calculateName(EN), name);
    }

    @Test
    public void testOodiIdFormat() {
        cur.id = CUR_ID_OODI;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, NAME_FI + SisuCourseUnitRealisation.SEPARATOR + SPECIFIER_FI)
            + generateSpan(SV, NAME_SV + SisuCourseUnitRealisation.SEPARATOR + SPECIFIER_SV)
            + generateSpan(EN, NAME_EN + SisuCourseUnitRealisation.SEPARATOR + SPECIFIER_EN), name);
    }

    @Test
    public void testOptimeIdFormat() {
        cur.id = CUR_ID_OPTIME;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, NAME_FI) + generateSpan(SV, NAME_SV) + generateSpan(EN, NAME_EN), name);
    }

    @Test
    public void testNonOptimeSisuOodiIdFormat() {
        cur.id = "123412341234";
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, cur.calculateName(FI))
            + generateSpan(SV, cur.calculateName(SV))
            + generateSpan(EN, cur.calculateName(EN)), name);

    }

    private String generateSpan(SisuLocale locale, String text) {
        return SPAN_START.replace(":lang:", locale.toString()) + text + SPAN_END;
    }
}
