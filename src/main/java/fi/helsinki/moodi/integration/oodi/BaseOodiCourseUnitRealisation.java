package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class BaseOodiCourseUnitRealisation implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("course_id")
    public Integer realisationId;

    @JsonProperty("end_date")
    public String endDate;

    @JsonProperty("deleted")
    public boolean removed;

    @JsonProperty("students")
    public List<OodiStudent> students = newArrayList();

    @JsonProperty("teachers")
    public List<OodiTeacher> teachers = newArrayList();
}
