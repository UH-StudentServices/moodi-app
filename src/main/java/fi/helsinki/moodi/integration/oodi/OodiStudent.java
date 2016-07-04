package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class OodiStudent {

    @JsonProperty("first_names")
    public String firstNames;

    @JsonProperty("student_number")
    public String studentNumber;

    @JsonProperty("mobile_phone")
    public String mobilePhone;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("email")
    public String email;

    @JsonProperty("enrollment_status_code")
    public Integer enrollmentStatusCode;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}