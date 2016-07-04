package fi.helsinki.moodi.integration.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public final class MoodleRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("roleid")
    public long roleId;

    @JsonProperty("name")
    public String name;

    @JsonProperty("shortname")
    public String shortName;
}
