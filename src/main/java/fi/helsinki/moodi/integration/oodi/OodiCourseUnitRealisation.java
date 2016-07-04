package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class OodiCourseUnitRealisation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("organisations")
    public List<OodiOrganisation> organisations = newArrayList();

    @JsonProperty("languages")
    public List<OodiLanguage> languages = newArrayList();

    @JsonProperty("credit_points")
    public Integer creditPoints;

    @JsonProperty("students")
    public List<OodiStudent> students = newArrayList();

    @JsonProperty("realisation_type_code")
    public Integer realisationTypeCode;

    @JsonProperty("enroll_end_date")
    public String enrollmentEndDate;

    @JsonProperty("start_date")
    public String startDate;

    @JsonProperty("descriptions")
    public List<OodiDescription> descriptions = newArrayList();

    @JsonProperty("realisation_name")
    public List<OodiLocalizedValue> realisationName = newArrayList();

    @JsonProperty("end_date")
    public String endDate;

    @JsonProperty("basecode")
    public String baseCode;

    @JsonProperty("teachers")
    public List<OodiTeacher> teachers = newArrayList();

    @JsonProperty("enroll_start_date")
    public String enrollmentStartDate;

    @JsonProperty("course_id")
    public Integer realisationId;

    @JsonProperty("last_changed")
    public String lastChanged;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}