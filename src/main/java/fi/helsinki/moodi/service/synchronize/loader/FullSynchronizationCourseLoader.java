package fi.helsinki.moodi.service.synchronize.loader;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation for full synchronization.
 */
@Component
public class FullSynchronizationCourseLoader implements CourseLoader {

    private final CourseService courseService;

    @Autowired
    public FullSynchronizationCourseLoader(CourseService courseService) {
        this.courseService = courseService;
    }

    @Override
    public List<Course> load() {
        return courseService.findAllCompleted();
    }

    @Override
    public SynchronizationType getType() {
        return SynchronizationType.FULL;
    }
}
