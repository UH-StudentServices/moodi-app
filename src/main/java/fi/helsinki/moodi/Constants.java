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

package fi.helsinki.moodi;

import java.util.Locale;

public final class Constants {

    public static final String LANG_FI = "fi";
    public static final String LANG_SV = "sv";
    public static final String LANG_EN = "en";
    public static final String LANG_DEFAULT = LANG_FI;
    public static final String LANG_FALLBACK = LANG_EN;

    public static final Locale LOCALE_FI = new Locale(LANG_FI);
    public static final Locale LOCALE_SV = new Locale(LANG_SV);
    public static final Locale LOCALE_EN = new Locale(LANG_EN);
    public static final Locale LOCALE_DEFAULT = LOCALE_FI;
    public static final Locale LOCALE_FALLBACK = LOCALE_EN;

    public static final Integer CONFIRMED_ENROLLMENT = 3;


    private Constants() {}
}
