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

import io.micrometer.core.instrument.util.StringUtils;

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

    public String getForLocaleOrFirstAvailable(SisuLocale locale) {
        String forLocale = getForLocale(locale);
        if (forLocale == null) {
            forLocale = Arrays.stream(SisuLocale.values())
                .filter(srl -> !srl.equals(locale) && StringUtils.isNotEmpty(getForLocale(srl)))
                .map(this::getForLocale)
                .findFirst()
                .orElse("");
        }
        return forLocale;
    }
}
