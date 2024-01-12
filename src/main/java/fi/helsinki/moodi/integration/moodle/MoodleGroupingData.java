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
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodleGroupingData {
    private Long id;
    @JsonProperty("courseid")
    private Long courseId;

    private String name;
    private String description;
    @JsonProperty("descriptionformat")
    private Long descriptionFormat = Constants.MOODLE_DESCRIPTION_FORMAT_HTML;

    @JsonProperty("idnumber")
    private String idNumber;

    private List<MoodleGroupData> groups = Collections.emptyList();

    public MoodleGroupingData(MoodleGrouping grouping) {
        this.id = grouping.getId();
        this.courseId = grouping.getCourseId();
        this.name = grouping.getName();
        this.description = grouping.getDescription();
        this.descriptionFormat = grouping.getDescriptionFormat();
        this.idNumber = grouping.getIdNumber();
        this.groups = grouping.getGroups().stream().map(MoodleGroupData::new).collect(Collectors.toList());
    }
}
