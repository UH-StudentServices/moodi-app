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

package fi.helsinki.moodi.service.synchronize.snapshot;

import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseSyncParticipantService {

    private final CourseSyncParticipantRepository courseSyncParticipantRepository;
    private final TimeService timeService;

    @Autowired
    public CourseSyncParticipantService(CourseSyncParticipantRepository courseSyncParticipantRepository,
                                        TimeService timeService) {
        this.courseSyncParticipantRepository = courseSyncParticipantRepository;
        this.timeService = timeService;
    }

    public Map<String, CourseSyncParticipant> getSnapshotByUsername(Long courseId) {
        if (courseId == null) {
            return Map.of();
        }
        return courseSyncParticipantRepository.findByCourseId(courseId).stream()
            .collect(Collectors.toMap(p -> p.username, Function.identity(), (a, b) -> b));
    }

    public void upsertAll(Long courseId, List<CourseSyncParticipant> participants) {
        if (courseId == null || participants.isEmpty()) {
            return;
        }
        final Map<String, CourseSyncParticipant> existingByUsername = new HashMap<>(getSnapshotByUsername(courseId));
        final LocalDateTime now = timeService.getCurrentUTCDateTime();

        for (CourseSyncParticipant participant : participants) {
            final CourseSyncParticipant existing = existingByUsername.get(participant.username);
            if (existing != null) {
                existing.moodleUserId = participant.moodleUserId != null ? participant.moodleUserId : existing.moodleUserId;
                existing.student = participant.student;
                existing.enrolledStudent = participant.enrolledStudent;
                existing.teacher = participant.teacher;
                existing.modified = now;
                courseSyncParticipantRepository.save(existing);
            } else {
                participant.created = now;
                participant.modified = now;
                courseSyncParticipantRepository.save(participant);
                existingByUsername.put(participant.username, participant);
            }
        }
    }

    public void deleteByCourseId(Long courseId) {
        if (courseId != null) {
            courseSyncParticipantRepository.deleteByCourseId(courseId);
        }
    }
}
