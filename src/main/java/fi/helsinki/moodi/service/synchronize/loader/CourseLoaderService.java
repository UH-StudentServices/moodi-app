package fi.helsinki.moodi.service.synchronize.loader;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Orchestrates course loading.
 *
 * @see CourseLoader
 */
@Service
public class CourseLoaderService {

    private final Map<SynchronizationType, CourseLoader> courseLoadersByType;

    @Autowired
    public CourseLoaderService(List<CourseLoader> courseLoaders) {
        this.courseLoadersByType = courseLoaders.stream()
                .collect(toMap(CourseLoader::getType, Function.identity(), (a, b) -> b));
    }

    /**
     * Load courses by synchronization type
     */
    public List<Course> load(final SynchronizationType type) {
        return courseLoadersByType.get(type).load();
    }
}
