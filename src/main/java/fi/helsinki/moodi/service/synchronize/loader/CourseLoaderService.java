/*
 * This file is part of Moodi application.
 *
 * Moodi application is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moodi application is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Moodi application.  If not, see <http://www.gnu.org/licenses/>.
 */

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
