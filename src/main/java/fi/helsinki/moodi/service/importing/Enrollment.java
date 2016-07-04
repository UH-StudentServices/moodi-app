package fi.helsinki.moodi.service.importing;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Optional;

public final class Enrollment implements Serializable {

    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";

    private static final long serialVersionUID = 1L;

    public String role;
    public Optional<String> teacherId;
    public Optional<String> studentNumber;
    public Optional<Long> moodleId;
    public Optional<String> username;

    public static Enrollment forStudent(final String studentNumber) {
        return new Enrollment(ROLE_STUDENT, Optional.empty(), Optional.of(studentNumber), Optional.empty(), Optional.empty());
    }

    public static Enrollment forTeacher(final String teacherId) {
        return new Enrollment(ROLE_TEACHER, Optional.of(teacherId), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private Enrollment(String role, Optional<String> teacherId, Optional<String> studentNumber, Optional<String> username, Optional<Long> moodleId) {
        this.role = role;
        this.teacherId = teacherId;
        this.studentNumber = studentNumber;
        this.username = username;
        this.moodleId = moodleId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
