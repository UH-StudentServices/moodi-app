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

public final class MoodleFullCourse {

    @JsonProperty("id")
    public Long id;

    @JsonProperty("idnumber")
    public String idNumber;

    @JsonProperty("shortname")
    public String shortName;

    @JsonProperty("fullname")
    public String fullName;

    @JsonProperty("displayname")
    public String displayName;

    @JsonProperty("summary")
    public String summary;

    @JsonProperty("startdate")
    public Long startDate;

    @JsonProperty("enddate")
    public Long endDate;

    @JsonProperty("lang")
    public String lang;

}
