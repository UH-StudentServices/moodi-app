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
import fi.helsinki.moodi.service.syncLock.SyncLockService;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnlockingSynchronizationCourseLoader implements CourseLoader {

    private final SyncLockService syncLockService;

    @Autowired
    public UnlockingSynchronizationCourseLoader(SyncLockService syncLockService) {
        this.syncLockService = syncLockService;
    }

    @Override
    public List<Course> load() {
        return syncLockService.getAndUnlockLockedCourses();
    }

    @Override
    public SynchronizationType getType() {
        return SynchronizationType.UNLOCK;
    }
}
