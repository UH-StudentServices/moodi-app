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

    public static final String DEFAULT_FI = "suomi";
    public static final String DEFAULT_SV = "ruotsi";
    public static final String DEFAULT_EN = "englanti";
    public static final String SPAN_START = "<span lang=\":lang:\" class=\"multilang\">";
    public static final String SPAN_END = "</span>";
    public SisuCourseUnitRealisation cur;

    @Before
    public void setUp() {
        cur = new SisuCourseUnitRealisation();
        cur.name = new SisuLocalisedValue(DEFAULT_FI, DEFAULT_SV, DEFAULT_EN);
    }

    @Test
    public void testNameLocalizationAllLanguages() {
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, DEFAULT_FI) + generateSpan(SV, DEFAULT_SV) + generateSpan(EN, DEFAULT_EN), name);
    }

    @Test
    public void testNameLocalizationTwoLanguages() {
        cur.name.sv = null;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, DEFAULT_FI) + generateSpan(EN, DEFAULT_EN), name);
    }

    @Test
    public void testNameLocalizationTwoLanguagesOtherParams() {
        cur.name.fi = null;
        String name = cur.generateName(SV);
        assertEquals(generateSpan(SV, DEFAULT_SV) + generateSpan(EN, DEFAULT_EN), name);
    }

    @Test
    public void testNameLocalizationOnlyTeachingLanguage() {
        cur.name.sv = null;
        cur.name.en = null;
        String name = cur.generateName(FI);
        assertEquals(DEFAULT_FI, name);
    }

    @Test
    public void testNameLocalizationAllIdentical() {
        cur.name.sv = DEFAULT_FI;
        cur.name.en = DEFAULT_FI;
        String name = cur.generateName(FI);
        assertEquals(DEFAULT_FI, name);
    }

    @Test
    public void testOneLocalizationIsIdenticalToDefault() {
        cur.name.sv = DEFAULT_FI;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(FI, DEFAULT_FI) + generateSpan(EN, DEFAULT_EN), name);
    }

    @Test
    public void testDefaultLocalizationMissingButThereAreTwoOtherOptions() {
        cur.name.fi = null;
        String name = cur.generateName(FI);
        assertEquals(generateSpan(SV, DEFAULT_SV) + generateSpan(EN, DEFAULT_EN), name);
    }

    @Test
    public void testDefaultLocalizationMissingButThereIsAnotherOption() {
        cur.name.fi = null;
        cur.name.sv = null;
        String name = cur.generateName(FI);
        assertEquals(DEFAULT_EN, name);
    }

    @Test
    public void testNameTooLong() {
        cur.name.sv = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        String name = cur.generateName(EN);
        assertEquals(DEFAULT_EN, name);
    }

    private String generateSpan(SisuLocale locale, String text) {
        return SPAN_START.replace(":lang:", locale.toString()) + text + SPAN_END;
    }
}
