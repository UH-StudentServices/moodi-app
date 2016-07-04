package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public class OodiTeacher implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("full_name")
    public String fullName;

    @JsonProperty("first_names")
    public String firstNames;

    @JsonProperty("teacher_id")
    public String teacherId;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("calling_name")
    public String callingName;

    @JsonProperty("email")
    public String email;

    @JsonProperty("teacher_role_code")
    public Integer teacherRoleCode;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}