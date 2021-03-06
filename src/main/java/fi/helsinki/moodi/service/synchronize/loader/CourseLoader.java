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

import java.util.List;

/**
 * Loads courses to synchronize. There should be one implementation
 * for each {@link SynchronizationType}.
 *
 * @see SynchronizationType
 */
public interface CourseLoader {

    /**
     * Get list of courses to synchronize.
     */
    List<Course> load();

    /**
     * Get type.
     */
    SynchronizationType getType();
}
