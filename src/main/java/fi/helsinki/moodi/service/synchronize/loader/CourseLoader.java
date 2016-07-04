package fi.helsinki.moodi.service.synchronize.loader;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;

import java.util.List;

/**
 * Loads courses to synchronize. There should be one implementation
 * for each {@link SynchronizationType}.
 *
 * @see SynchronizationType
 */
public interface CourseLoader {

    /**
     * Get list of courses to synchronize
     */
    List<Course> load();

    /**
     * Get type
     */
    SynchronizationType getType();
}
