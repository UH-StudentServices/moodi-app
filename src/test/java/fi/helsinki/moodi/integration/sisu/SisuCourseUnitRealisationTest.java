package fi.helsinki.moodi.integration.sisu;

import org.junit.Before;
import org.junit.Test;
import static fi.helsinki.moodi.integration.sisu.SisuLocale.FI;
import static fi.helsinki.moodi.integration.sisu.SisuLocale.SV;
import static fi.helsinki.moodi.integration.sisu.SisuLocale.EN;
import static org.junit.Assert.assertEquals;

public class SisuCourseUnitRealisationTest {

    public static final String DEFAULT_FI = "suomi";
    public static final String DEFAULT_SV = "ruosi";
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

    @Test(expected = NullPointerException.class)
    public void testNameOnlyOtherName() {
        cur.name.sv = null;
        cur.name.fi = null;
        String name = cur.generateName(FI);
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
