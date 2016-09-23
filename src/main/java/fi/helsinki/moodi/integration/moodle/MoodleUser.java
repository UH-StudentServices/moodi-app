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
import com.fasterxml.jackson.annotation.JsonView;
import fi.helsinki.moodi.service.util.JsonViews;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.List;

public final class MoodleUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonView(JsonViews.FileLogging.class)
    @JsonProperty("id")
    public Long id;

    @JsonProperty("username")
    public String username;

    @JsonProperty("email")
    public String email;

    @JsonProperty("fullname")
    public String fullname;

    @JsonProperty("roles")
    public List<MoodleRole> roles;

    public MoodleUser() {
    }

    public MoodleUser(Long id, String username, String email, String fullname) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullname = fullname;
    }

    public boolean hasRole(long roleId) {
        if (roles == null) {
            return false;
        } else {
            return roles.stream().anyMatch(r -> r.roleId == roleId);
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
