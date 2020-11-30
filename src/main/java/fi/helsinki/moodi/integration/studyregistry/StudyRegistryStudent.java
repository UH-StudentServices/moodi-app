package fi.helsinki.moodi.integration.studyregistry;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class StudyRegistryStudent {
    public String firstNames;

    public String studentNumber;

    // Eg. jdoe@helsinki.fi
    public String userName;

    public String lastName;

    public boolean isEnrolled;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
