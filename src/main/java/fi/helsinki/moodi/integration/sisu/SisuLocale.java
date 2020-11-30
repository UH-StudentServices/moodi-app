package fi.helsinki.moodi.integration.sisu;

public enum SisuLocale {
    FI("fi"),

    SV("sv"),

    EN("en");

    private final String localeString;

    SisuLocale(String localeString) {
        this.localeString = localeString;
    }

    public static SisuLocale byCodeOrDefaultToFi(String languageCode) {
        for(SisuLocale l : values()) {
            if (l.localeString.equals(languageCode)) {
                return l;
            }
        }
        return FI;
    }

    public static SisuLocale byUrnOrDefaultToFi(String languageUrn) {
        String localeString = languageUrn.substring(languageUrn.lastIndexOf(':') + 1);
        return byCodeOrDefaultToFi(localeString);
    }

    @Override
    public String toString() {
        return localeString;
    }
}
