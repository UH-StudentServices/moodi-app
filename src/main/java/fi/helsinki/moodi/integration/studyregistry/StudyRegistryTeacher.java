package fi.helsinki.moodi.integration.studyregistry;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class StudyRegistryTeacher {
    public String firstNames;

    // Eg. jdoe@helsinki.fi
    public String userName;

    public String employeeNumber;

    public String lastName;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
