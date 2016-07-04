package fi.helsinki.moodi.integration.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.List;

public final class MoodleUserEnrollments implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    public Long id;

    @JsonProperty("roles")
    public List<MoodleRole> roles;

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
