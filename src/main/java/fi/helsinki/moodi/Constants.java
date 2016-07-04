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
