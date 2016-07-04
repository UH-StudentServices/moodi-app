package fi.helsinki.moodi.integration.oodi;

public class OodiLocalizedValue {
    public OodiLocale langcode;
    public String text;

    public OodiLocalizedValue() {
    }

    public OodiLocalizedValue(OodiLocale langcode, String text) {
        this.langcode = langcode;
        this.text = text;
    }
}
