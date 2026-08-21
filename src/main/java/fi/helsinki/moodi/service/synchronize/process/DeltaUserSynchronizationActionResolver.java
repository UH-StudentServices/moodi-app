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

package fi.helsinki.moodi.service.synchronize.process;

import com.google.common.collect.Sets;
import fi.helsinki.moodi.service.synchronize.snapshot.CourseSyncParticipant;
import fi.helsinki.moodi.service.util.MapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Resolves user synchronization actions by comparing the current Sisu state against the
 * locally stored snapshot of the last successfully synced Sisu state (CourseSyncParticipant),
 * without consulting the live Moodle enrollment state. Manual enrollments and removals
 * done in Moodle are intentionally not visible to and not touched by this resolver.
 */
@Component
public class DeltaUserSynchronizationActionResolver {

    private final MapperService mapperService;

    @Autowired
    public DeltaUserSynchronizationActionResolver(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    public UserSynchronizationItem enrichWithActions(final UserSynchronizationItem item) {
        final CourseSyncParticipant snapshot = item.getExistingSnapshot();

        final boolean isEnrolledStudent = item.getStudent() != null && item.getStudent().isEnrolled;
        final boolean isTeacher = item.getTeacher() != null;
        final boolean wasEnrolledStudent = snapshot != null && snapshot.enrolledStudent;
        final boolean wasTeacher = snapshot != null && snapshot.teacher;

        final List<UserSynchronizationAction> actions = newArrayList();

        if (snapshot == null) {
            // New user for this course: a single idempotent enrollment covers students,
            // teachers and hybrids alike.
            Set<Long> roles = Sets.newHashSet();
            if (isEnrolledStudent) {
                roles.add(mapperService.getStudentRoleId());
            }
            if (isTeacher) {
                roles.add(mapperService.getTeacherRoleId());
            }
            if (!roles.isEmpty()) {
                roles.add(mapperService.getMoodiRoleId());
                addAction(item, roles, UserSynchronizationActionType.ADD_ENROLLMENT, actions);
            }
        } else {
            if (!wasEnrolledStudent && isEnrolledStudent) {
                // Re-enrolling in Moodle clears the suspension flag, so reactivation needs no
                // knowledge of the current Moodle state.
                addAction(item, Sets.newHashSet(mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()),
                    UserSynchronizationActionType.REACTIVATE_ENROLLMENT, actions);
            }

            if (wasEnrolledStudent && !isEnrolledStudent) {
                if (!isTeacher && !wasTeacher) {
                    // If the user was manually removed from the Moodle course, this re-creates the
                    // enrollment as suspended (invisible to the user) - an accepted trade-off of
                    // not reading the Moodle state.
                    addAction(item, Sets.newHashSet(mapperService.getMoodiRoleId()),
                        UserSynchronizationActionType.SUSPEND_ENROLLMENT, actions);
                }
                // Teachers are never suspended; for everyone the student role is taken away,
                // keeping up the convention that a suspended student has only the sync role.
                addAction(item, Sets.newHashSet(mapperService.getStudentRoleId()),
                    UserSynchronizationActionType.REMOVE_ROLES, actions);
            }

            if (!wasTeacher && isTeacher) {
                addAction(item, Sets.newHashSet(mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()),
                    UserSynchronizationActionType.ADD_ENROLLMENT, actions);
            }
            // wasTeacher && !isTeacher: the teacher role is never removed (same policy as the
            // live-state resolver); the snapshot update alone records the change.
        }

        return item.withActions(actions);
    }

    private void addAction(UserSynchronizationItem item, Set<Long> roles, UserSynchronizationActionType type,
                           List<UserSynchronizationAction> actions) {
        actions.add(new UserSynchronizationAction(type, roles, item.getMoodleUserId()));
    }
}
