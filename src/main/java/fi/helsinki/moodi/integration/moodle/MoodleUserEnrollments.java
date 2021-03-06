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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public final class MoodleUserEnrollments {

    @JsonProperty("id")
    public Long id;

    @JsonProperty("username")
    public String username;

    @JsonProperty("roles")
    public List<MoodleRole> roles = newArrayList();

    @JsonProperty("enrolledcourses")
    public List<MoodleCourseData> enrolledCourses = newArrayList();

    public boolean hasRole(long roleId) {
        if (roles == null) {
            return false;
        } else {
            return roles.stream().anyMatch(r -> r.roleId == roleId);
        }
    }

    public boolean seesCourse(Long courseId) {
        return enrolledCourses == null ? false : enrolledCourses.stream().anyMatch(e -> e.id == courseId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
