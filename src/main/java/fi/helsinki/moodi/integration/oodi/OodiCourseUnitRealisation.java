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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class OodiCourseUnitRealisation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("organisations")
    public List<OodiOrganisation> organisations = newArrayList();

    @JsonProperty("languages")
    public List<OodiLanguage> languages = newArrayList();

    @JsonProperty("credit_points")
    public Integer creditPoints;

    @JsonProperty("students")
    public List<OodiStudent> students = newArrayList();

    @JsonProperty("realisation_type_code")
    public Integer realisationTypeCode;

    @JsonProperty("enroll_end_date")
    public String enrollmentEndDate;

    @JsonProperty("start_date")
    public String startDate;

    @JsonProperty("descriptions")
    public List<OodiDescription> descriptions = newArrayList();

    @JsonProperty("realisation_name")
    public List<OodiLocalizedValue> realisationName = newArrayList();

    @JsonProperty("end_date")
    public String endDate;

    @JsonProperty("basecode")
    public String baseCode;

    @JsonProperty("teachers")
    public List<OodiTeacher> teachers = newArrayList();

    @JsonProperty("enroll_start_date")
    public String enrollmentStartDate;

    @JsonProperty("course_id")
    public Integer realisationId;

    @JsonProperty("last_changed")
    public String lastChanged;

    @JsonProperty("deleted")
    public boolean removed;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}