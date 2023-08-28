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
import fi.helsinki.moodi.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodleGroupData {

    private Long id;

    @JsonProperty("courseid")
    private Long courseId;
    private String name;
    private String description;
    @JsonProperty("descriptionformat")
    private Long descriptionFormat = Constants.MOODLE_DESCRIPTION_FORMAT_HTML;
    @JsonProperty("enrolmentkey")
    private String enrolmentKey;
    @JsonProperty("idnumber")
    private String idNumber;
    List<MoodleUser> members = Collections.emptyList();

    public MoodleGroupData(MoodleGroup group) {
        this.id = group.getId();
        this.courseId = group.getCourseId();
        this.name = group.getName();
        this.description = group.getDescription();
        this.descriptionFormat = group.getDescriptionFormat();
        this.enrolmentKey = group.getEnrolmentKey();
        this.idNumber = group.getIdNumber();
        this.members = group.getMembers();
    }
}
