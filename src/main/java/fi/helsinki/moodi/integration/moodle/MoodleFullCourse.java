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