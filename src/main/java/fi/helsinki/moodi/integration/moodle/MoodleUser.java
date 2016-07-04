package fi.helsinki.moodi.integration.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.List;

public final class MoodleUser implements Serializable {

    private static final long serialVersionUID = 1L;

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
