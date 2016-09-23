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

package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import fi.helsinki.moodi.service.util.JsonViews;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class OodiStudent {

    @JsonProperty("first_names")
    public String firstNames;

    @JsonView(JsonViews.FileLogging.class)
    @JsonProperty("student_number")
    public String studentNumber;

    @JsonProperty("mobile_phone")
    public String mobilePhone;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("email")
    public String email;

    @JsonProperty("enrollment_status_code")
    public Integer enrollmentStatusCode;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}