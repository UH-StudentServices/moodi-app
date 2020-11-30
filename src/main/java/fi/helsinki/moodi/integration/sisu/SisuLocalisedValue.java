package fi.helsinki.moodi.integration.sisu;

import java.util.Arrays;

public class SisuLocalisedValue {

    public String fi;
    public String en;
    public String sv;

    public SisuLocalisedValue() {

    }

    public SisuLocalisedValue(String fi, String sv, String en) {
        this.fi = fi;
        this.sv = sv;
        this.en = en;
    }

    public String getForLocale(SisuLocale locale) {
        switch (locale) {
            case FI: return fi;
            case EN: return en;
            case SV: return sv;
            default: return null;
        }
    }

    public String getForLocaleOrDefault(SisuLocale locale) {
        String forLocale = getForLocale(locale);
        if (forLocale == null) {
            forLocale = Arrays.stream(SisuLocale.values())
                .filter(srl -> !srl.equals(locale))
                .map(this::getForLocale)
                .findFirst()
                .orElse(null);
        }
        return forLocale;
    }
}
