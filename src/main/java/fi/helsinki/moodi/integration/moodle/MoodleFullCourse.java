package fi.helsinki.moodi.integration.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public final class MoodleFullCourse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("groupmode")
    public Integer groupMode;
    @JsonProperty("idnumber")
    public String idNumber;
    @JsonProperty("summaryformat")
    public Integer summaryFormat;
    @JsonProperty("showreports")
    public Integer showReports;
    @JsonProperty("startdate")
    public Integer startDate;
    @JsonProperty("numsections")
    public Integer numSections;
    @JsonProperty("completionnotify")
    public Integer completionNotify;
    @JsonProperty("defaultgroupingid")
    public Integer defaultGroupingId;
    @JsonProperty("showgrades")
    public Integer showGrades;
    @JsonProperty("forcetheme")
    public String forceTheme;
    @JsonProperty("id")
    public Long id;
    @JsonProperty("lang")
    public String lang;
    @JsonProperty("categoryid")
    public Integer categoryId;
    @JsonProperty("summary")
    public String summary;
    @JsonProperty("visible")
    public Integer visible;
    @JsonProperty("format")
    public String format;
    @JsonProperty("categorysortorder")
    public Integer categorySortOrder;
    @JsonProperty("hiddensections")
    public Integer hiddenSections;
    @JsonProperty("groupmodeforce")
    public Integer groupModeForce;
    @JsonProperty("shortname")
    public String shortName;
    @JsonProperty("enablecompletion")
    public Integer enableCompletion;
    @JsonProperty("newsitems")
    public Integer newsItems;
    @JsonProperty("timecreated")
    public Integer timeCreated;
    @JsonProperty("timemodified")
    public Integer timeModified;
    @JsonProperty("maxbytes")
    public Integer maxBytes;
    @JsonProperty("fullname")
    public String fullName;
}