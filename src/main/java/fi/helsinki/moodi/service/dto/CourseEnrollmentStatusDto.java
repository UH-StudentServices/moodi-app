package fi.helsinki.moodi.service.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CourseEnrollmentStatusDto {

    public List<TeacherEnrollmentStatusDto> teacherEnrollments;

    public List<StudentEnrollmentStatusDto> studentEnrollments;

    public LocalDateTime created;

}
