package fi.helsinki.moodi.service.converter;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.dto.CourseDto;
import fi.helsinki.moodi.service.dto.CourseEnrollmentStatusDto;
import fi.helsinki.moodi.service.util.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CourseConverter {

    private final UrlBuilder urlBuilder;

    @Autowired
    public CourseConverter(UrlBuilder urlBuilder) {
        this.urlBuilder = urlBuilder;
    }

    public CourseDto toDto(Course course, CourseEnrollmentStatusDto courseEnrollmentStatus) {
        CourseDto courseDto = new CourseDto();
        courseDto.url = urlBuilder.getMoodleCourseUrl(course.moodleId);
        courseDto.importStatus = course.importStatus.toString();
        courseDto.courseEnrollmentStatus = courseEnrollmentStatus;
        return courseDto;
    }
}
