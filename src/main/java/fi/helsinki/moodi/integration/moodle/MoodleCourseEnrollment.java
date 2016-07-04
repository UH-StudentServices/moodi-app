package fi.helsinki.moodi.integration.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public final class MoodleCourseEnrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    public Long id;

    @JsonProperty("fullname")
    public String fullName;

    @JsonProperty("shortname")
    public String shortName;

}
