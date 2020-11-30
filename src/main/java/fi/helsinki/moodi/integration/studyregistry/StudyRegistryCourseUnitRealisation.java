package fi.helsinki.moodi.integration.studyregistry;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDate;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class StudyRegistryCourseUnitRealisation {
    public String realisationId;

    public Origin origin;

    public LocalDate endDate;

    public LocalDate startDate;

    public boolean published;

    public List<StudyRegistryStudent> students = newArrayList();

    public List<StudyRegistryTeacher> teachers = newArrayList();

    public String description;

    public String realisationName;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public enum Origin {
        SISU, OODI
    }

}
