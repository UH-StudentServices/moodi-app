package fi.helsinki.moodi.service.dto.converter;


import com.fasterxml.jackson.core.type.TypeReference;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatus;
import fi.helsinki.moodi.service.dto.CourseEnrollmentStatusDto;
import fi.helsinki.moodi.service.dto.StudentEnrollmentStatusDto;
import fi.helsinki.moodi.service.dto.TeacherEnrollmentStatusDto;
import fi.helsinki.moodi.service.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseEnrollmentStatusDtoConverter {

    private final JsonUtil jsonUtil;

    @Autowired
    public CourseEnrollmentStatusDtoConverter(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    public CourseEnrollmentStatusDto toDto(CourseEnrollmentStatus courseEnrollmentStatus) {
        CourseEnrollmentStatusDto courseEnrollmentStatusDto = new CourseEnrollmentStatusDto();
        courseEnrollmentStatusDto.created = courseEnrollmentStatus.created;
        try {
            courseEnrollmentStatusDto.studentEnrollments =
                jsonUtil.getObjectMapper().readValue(courseEnrollmentStatus.studentEnrollments, new TypeReference<List<StudentEnrollmentStatusDto>>() {});
            courseEnrollmentStatusDto.teacherEnrollments =
                jsonUtil.getObjectMapper().readValue(courseEnrollmentStatus.teacherEnrollments, new TypeReference<List<TeacherEnrollmentStatusDto>>() {});
        } catch (Exception e) {}

        return courseEnrollmentStatusDto;

    }

}
