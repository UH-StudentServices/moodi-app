package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public class OodiCourseChange implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("course_id")
    public long courseUnitRealisationId;

    @JsonProperty("learningopportunity_id")
    public String learningOpportunityId;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}