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
import fi.helsinki.moodi.integration.oodi.OodiStudentApprovalStatusResolver;
import fi.helsinki.moodi.service.util.MapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class UserSynchronizationActionResolver {

    private final MapperService mapperService;
    private final OodiStudentApprovalStatusResolver oodiStudentApprovalStatusResolver;

    @Autowired
    public UserSynchronizationActionResolver(MapperService mapperService,
                                             OodiStudentApprovalStatusResolver oodiStudentApprovalStatusResolver) {
        this.mapperService = mapperService;
        this.oodiStudentApprovalStatusResolver = oodiStudentApprovalStatusResolver;
    }

    private Set<Long> getCurrentOodiRoles(UserSynchronizationItem item) {
        Set<Long> currentOodiRoles = Sets.newHashSet();

        if (item.getOodiStudent() != null && oodiStudentApprovalStatusResolver.isApproved(item.getOodiStudent())) {
            currentOodiRoles.add(mapperService.getStudentRoleId());
        }

        if (item.getOodiTeacher() != null) {
            currentOodiRoles.add(mapperService.getTeacherRoleId());
        }

        return currentOodiRoles;
    }

    private Set<Long> addDefaultRoleIfNotEmpty(Set<Long> roles) {
        if (roles.size() > 0) {
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

        return null;
    }

    private List<UserSynchronizationAction> createEnrollmentActions(Long moodleUserId, Set<Long> currentRolesInOodi) {
        return addAction(moodleUserId, currentRolesInOodi, UserSynchronizationActionType.ADD_ENROLLMENT, newArrayList());
    }

    private List<UserSynchronizationAction> createRoleChangeAndSuspendActions(Long moodleUserId,
                                                                              Set<Long> currentRolesInOodi,
                                                                              Set<Long> currentRolesInMoodle) {
        Set<Long> rolesToAdd = difference(currentRolesInOodi, currentRolesInMoodle);
        Set<Long> rolesToRemove = difference(currentRolesInMoodle, currentRolesInOodi).stream()
            .filter(this::roleCanBeRemoved)
            .collect(Collectors.toSet());

        List<UserSynchronizationAction> actions = newArrayList();

        addAction(moodleUserId, rolesToAdd, UserSynchronizationActionType.ADD_ROLES, actions);

        boolean suspendStudent = (rolesToRemove.contains(mapperService.getStudentRoleId()) &&
                !currentRolesInMoodle.contains(mapperService.getTeacherRoleId()));

        if (suspendStudent) {
            addAction(moodleUserId, Sets.newHashSet(mapperService.getMoodiRoleId()), UserSynchronizationActionType.SUSPEND_ENROLLMENT, actions);
        }

        addAction(moodleUserId, rolesToRemove, UserSynchronizationActionType.REMOVE_ROLES, actions);

        return actions;
    }

    private Set<Long> difference(Set<Long> list1, Set<Long> list2) {
        return list1.stream().filter(item -> !list2.contains(item)).collect(Collectors.toSet());
    }

    private List<UserSynchronizationAction> addAction(Long moodleUserId,
                                                      Set<Long> roles,
                                                      UserSynchronizationActionType type,
                                                      List<UserSynchronizationAction> actions) {
        if (type == UserSynchronizationActionType.SUSPEND_ENROLLMENT || !roles.isEmpty()) {
            actions.add(new UserSynchronizationAction(type, roles, moodleUserId));
        }
        return actions;
    }

    public UserSynchronizationItem enrichWithActions(final UserSynchronizationItem item) {
        Set<Long> currentRolesInOodiWithDefaultRole = addDefaultRoleIfNotEmpty(getCurrentOodiRoles(item));
        Long moodleUserId = item.getMoodleUserId();

        if (item.getMoodleUserEnrollments() != null) {
            return item.withActions(createRoleChangeAndSuspendActions(moodleUserId, currentRolesInOodiWithDefaultRole, getCurrentMoodleRoles(item)));
        } else {
            return item.withActions(createEnrollmentActions(moodleUserId, currentRolesInOodiWithDefaultRole));
        }
    }
}
