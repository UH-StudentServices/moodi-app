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

import java.util.List;

public final class MoodleCourseWithEnrollments {

    @JsonProperty("courseid")
    public Long courseid;

    @JsonProperty("users")
    public List<MoodleUserEnrollments> users;

    // Default constructor needed for deserialization
    public MoodleCourseWithEnrollments() {
    }

    public MoodleCourseWithEnrollments(Long courseid, List<MoodleUserEnrollments> users) {
        this.courseid = courseid;
        this.users = users;
    }
}
