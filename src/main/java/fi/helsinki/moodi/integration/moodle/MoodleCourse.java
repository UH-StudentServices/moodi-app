package fi.helsinki.moodi.integration.moodle;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public final class MoodleCourse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String idnumber;
    public final String fullName;
    public final String shortName;
    public final String categoryId;
    public final String summary;
    public final String format;
    public final boolean showGrades;
    public final boolean visible;
    public final boolean showReports;
    public final int maxBytes;
    public final int newsItems;
    public final int numSections;

    public MoodleCourse(
            String idnumber,
            String fullName,
            String shortName,
            String categoryId,
            String summary,
            String format,
            boolean showGrades,
            boolean visible,
            boolean showReports,
            int maxBytes,
            int newsItems,
            int numSections) {

        this.idnumber = idnumber;
        this.fullName = fullName;
        this.shortName = shortName;
        this.categoryId = categoryId;
        this.summary = summary;
        this.format = format;
        this.maxBytes = maxBytes;
        this.showGrades = showGrades;
        this.visible = visible;
        this.newsItems = newsItems;
        this.numSections = numSections;
        this.showReports = showReports;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
