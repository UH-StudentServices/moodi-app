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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class OodiStudent {

    @JsonProperty("first_names")
    public String firstNames;

    @JsonProperty("student_number")
    public String studentNumber;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("approved")
    public boolean approved = true;

    @JsonProperty("automatic_enabled")
    public boolean automaticEnabled;

    @JsonProperty("enrollment_status_code")
    public int enrollmentStatusCode;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public StudyRegistryStudent toStudyRegistryStudent() {
        StudyRegistryStudent ret = new StudyRegistryStudent();
        ret.firstNames = firstNames;
        ret.lastName = lastName;
        ret.studentNumber = studentNumber;
        ret.isEnrolled = OodiStudentApprovalStatusResolver.isApproved(this);
        return ret;
    }
}
