package fi.helsinki.moodi.integration.moodle;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public final class MoodleEnrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    public long moodleRoleId;
    public long moodleUserId;
    public long moodleCourseId;

    public MoodleEnrollment(long moodleRoleId, long moodleUserId, long moodleCourseId) {
        this.moodleRoleId = moodleRoleId;
        this.moodleUserId = moodleUserId;
        this.moodleCourseId = moodleCourseId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}