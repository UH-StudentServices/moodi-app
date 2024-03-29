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
import fi.helsinki.moodi.service.util.MapperService;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class UserSynchronizationActionResolver {

    private final MapperService mapperService;

    @Autowired
    public UserSynchronizationActionResolver(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    private Set<Long> getCurrentStudyRegistryRoles(UserSynchronizationItem item) {
        Set<Long> currentRoles = Sets.newHashSet();

        if (item.getStudent() != null && item.getStudent().isEnrolled) {
            currentRoles.add(mapperService.getStudentRoleId());
        }

        if (item.getTeacher() != null) {
            currentRoles.add(mapperService.getTeacherRoleId());
        }

        return currentRoles;
    }

    private Set<Long> addDefaultRoleIfNotEmpty(Set<Long> roles) {
        if (!roles.isEmpty()) {
            roles.add(mapperService.getMoodiRoleId());
        }
        return roles;
    }

    private boolean roleCanBeRemoved(Long role) {
        // Teacher role and Sync role (MoodiRole) cannot be removed
        return role == mapperService.getStudentRoleId();
    }

    private Set<Long> getCurrentMoodleRoles(UserSynchronizationItem item) {
        if (item.getMoodleUserEnrollments() != null) {
            return item.getMoodleUserEnrollments().roles.stream()
                    .map(role -> role.roleId)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    private void addRoleChangeAndSuspendActions(Long moodleUserId, Set<Long> currentRegistryRoles, Set<Long> currentRolesInMoodle,
                                                   boolean userSeesCourseInMoodle, List<UserSynchronizationAction> actions) {
        Set<Long> rolesToAdd = difference(currentRegistryRoles, currentRolesInMoodle);
        // Check that the user has the synced (moodi) role indicating he is controlled by Moodi instead of
        // having been manually added to Moodle.
        Set<Long> rolesToRemove = currentRolesInMoodle.contains(mapperService.getMoodiRoleId()) ?
            difference(currentRolesInMoodle, currentRegistryRoles).stream()
            .filter(this::roleCanBeRemoved)
            .collect(Collectors.toSet()) :
            new HashSet<>();

        if (!rolesToAdd.isEmpty()) {
            addAction(moodleUserId, rolesToAdd, UserSynchronizationActionType.ADD_ROLES, actions);
        }

        // Suspend when
        //  the user can see the course AND
        //  user does not have teacher role in Moodle AND
        //  student role is removed from student registry OR (user only has the sync role in Moodle AND no student role in student registry)
        boolean suspendStudent =
            (rolesToRemove.contains(mapperService.getStudentRoleId()) ||
                (hasOnlySyncRole(currentRolesInMoodle) && !currentRegistryRoles.contains(mapperService.getStudentRoleId()))) &&
                !currentRolesInMoodle.contains(mapperService.getTeacherRoleId()) &&
                // Prevents user getting continuously suspended, as a suspended user does not see the course.
                // User sees course if field "enrolledcourses" in the response to core_enrol_get_enrolled_users contains the id
                // of the course.
                // Also, if the course is not visible in Moodle, user does not see it.
                userSeesCourseInMoodle;

        if (suspendStudent) {
            addAction(moodleUserId, Sets.newHashSet(mapperService.getMoodiRoleId()), UserSynchronizationActionType.SUSPEND_ENROLLMENT, actions);
        }

        // We identify a suspended student by him having an enrollment in Moodle with just the sync role.
        boolean reactivateStudent = rolesToAdd.contains(mapperService.getStudentRoleId()) && hasOnlySyncRole(currentRolesInMoodle);

        if (reactivateStudent) {
            addAction(moodleUserId, Sets.newHashSet(mapperService.getStudentRoleId()), UserSynchronizationActionType.REACTIVATE_ENROLLMENT, actions);
        }

        if (!rolesToRemove.isEmpty()) {
            addAction(moodleUserId, rolesToRemove, UserSynchronizationActionType.REMOVE_ROLES, actions);
        }
    }

    private boolean hasOnlySyncRole(Set<Long> roles) {
        return roles.contains(mapperService.getMoodiRoleId()) && roles.size() == 1;
    }

    private Set<Long> difference(Set<Long> list1, Set<Long> list2) {
        return list1.stream().filter(item -> !list2.contains(item)).collect(Collectors.toSet());
    }

    private void addAction(Long moodleUserId, Set<Long> roles, UserSynchronizationActionType type, List<UserSynchronizationAction> actions) {
        actions.add(new UserSynchronizationAction(type, roles, moodleUserId));
    }

    public UserSynchronizationItem enrichWithActions(final UserSynchronizationItem item) {
        Set<Long> currentStudyRegistryRolesWithDefaultRole = addDefaultRoleIfNotEmpty(getCurrentStudyRegistryRoles(item));
        Long moodleUserId = item.getMoodleUserId();
        List<UserSynchronizationAction> actions = newArrayList();
        if (item.getMoodleUserEnrollments() != null) {
            addRoleChangeAndSuspendActions(
                moodleUserId,
                currentStudyRegistryRolesWithDefaultRole,
                getCurrentMoodleRoles(item),
                item.userSeesCourseInMoodle(),
                actions
            );
        } else if (!currentStudyRegistryRolesWithDefaultRole.isEmpty()) {
            addAction(moodleUserId, currentStudyRegistryRolesWithDefaultRole, UserSynchronizationActionType.ADD_ENROLLMENT, actions);
        }
        return item.withActions(actions);
    }
}
