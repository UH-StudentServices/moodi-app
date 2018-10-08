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

package fi.helsinki.moodi.service.synclock;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SyncLockService {

    private final SyncLockRepository syncLockRepository;
    private final TimeService timeService;

    @Autowired
    public SyncLockService(SyncLockRepository syncLockRepository, TimeService timeService) {
        this.syncLockRepository = syncLockRepository;
        this.timeService = timeService;
    }

    public boolean isLocked(Course course) {
        return syncLockRepository.findByCourseIdAndActiveTrue(course.id).isPresent();
    }

    public void setLock(Course course, String reason) {
        LocalDateTime localDateTime = timeService.getCurrentDateTime();

        SyncLock syncLock = new SyncLock();
        syncLock.course = course;
        syncLock.reason = reason;
        syncLock.created = localDateTime;
        syncLock.modified = localDateTime;
        syncLockRepository.save(syncLock);
    }

    public List<Course> getAndUnlockLockedCourses() {
        List<SyncLock> locks = syncLockRepository.findAllByActiveTrue();

        locks.forEach(l -> {
            l.active = false;
            l.modified = timeService.getCurrentDateTime();
        });

        syncLockRepository.saveAll(locks);

        return locks
            .stream()
            .map(l -> l.course)
            .collect(Collectors.toList());
    }

}
