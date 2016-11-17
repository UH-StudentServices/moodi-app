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

package fi.helsinki.moodi.service.syncLock;

import fi.helsinki.moodi.service.course.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SyncLockService {

    private final SyncLockRepository syncLockRepository;

    @Autowired
    public SyncLockService(SyncLockRepository syncLockRepository) {
        this.syncLockRepository = syncLockRepository;
    }

    public boolean isLocked(Course course) {
        return syncLockRepository.findByCourseIdAndActiveTrue(course.id).isPresent();
    }

    public void setLock(Course course, String reason) {
        SyncLock syncLock = new SyncLock();
        syncLock.course = course;
        syncLock.reason = reason;
        syncLockRepository.save(syncLock);
    }

    public List<Course> getAndUnlockLockedCourses() {
        List<SyncLock> locks = syncLockRepository.findAllByActiveTrue();

        locks.forEach(l -> l.active = false);

        syncLockRepository.save(locks);

        return locks
            .stream()
            .map(l -> l.course)
            .collect(Collectors.toList());
    }


}
