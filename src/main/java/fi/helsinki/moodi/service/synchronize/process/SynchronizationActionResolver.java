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

import fi.helsinki.moodi.service.util.MapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class SynchronizationActionResolver {

    private final MapperService mapperService;

    @Autowired
    public SynchronizationActionResolver(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    private List<Long> getCurrentOodiRoles(UserSynchronizationItem item) {
        List<Long> currentOodiRoles = newArrayList();

        if(item.getOodiStudent() != null && item.getOodiStudent().approved) {
            currentOodiRoles.add(mapperService.getStudentRoleId());
        }

        if(item.getOodiTeacher() != null) {
            currentOodiRoles.add(mapperService.getTeacherRoleId());
        }

        return currentOodiRoles;
    }

    private List<Long> addDefaultRole(List<Long> roles) {
        roles.add(mapperService.getMoodiRoleId());
        return roles;
    }

    private boolean roleCanBeRemoved(Long role) {
       return role == mapperService.getStudentRoleId();
    }

    private List<Long> getCurrentMoodleRoles(UserSynchronizationItem item) {
        if(item.getMoodleUserEnrollments() != null) {
            return item.getMoodleUserEnrollments().roles.stream()
                .map(role-> role.roleId)
                .collect(Collectors.toList());
        }

        return null;
    }


    private List<UserSynchronizationAction> createEnrollmentActions(Long moodleUserId, List<Long> currentRolesInOodi) {
        return addAction(moodleUserId, currentRolesInOodi, UserSynchronizationActionType.ADD_ENROLLMENT_WITH_ROLES, newArrayList());
    }

    private List<UserSynchronizationAction> createRoleChangeActions(Long moodleUserId, List<Long> currentRolesInOodi, List<Long> currentRolesInMoodle) {
        List<Long> rolesToAdd = difference(currentRolesInOodi, currentRolesInMoodle);
        List<Long> rolesToRemove = difference(currentRolesInMoodle, currentRolesInOodi).stream()
            .filter(this::roleCanBeRemoved)
            .collect(Collectors.toList());

        List<UserSynchronizationAction> actions = newArrayList();

        addAction(moodleUserId, rolesToAdd, UserSynchronizationActionType.ADD_ROLES, actions);
        addAction(moodleUserId, rolesToRemove, UserSynchronizationActionType.REMOVE_ROLES, actions);

        return actions;
    }

    private List<Long> difference(List<Long> list1, List<Long> list2) {
        return list1.stream().filter(item -> !list2.contains(item)).collect(Collectors.toList());
    }

    private List<UserSynchronizationAction> addAction(Long moodleUserId, List<Long> roles, UserSynchronizationActionType type, List<UserSynchronizationAction> actions) {
        if(roles.size() > 0) {
            actions.add(new UserSynchronizationAction(type, roles, moodleUserId));
        }
        return actions;
    }

    public UserSynchronizationItem enrichWithActions(final UserSynchronizationItem item) {
       List<Long> currentRolesInOodiWithDefaultRole = addDefaultRole(getCurrentOodiRoles(item));
       Long moodleUserId = item.getMoodleUserId();

       if(item.getMoodleUserEnrollments() != null) {
           return item.withActions(createRoleChangeActions(moodleUserId, currentRolesInOodiWithDefaultRole, getCurrentMoodleRoles(item)));
       } else {
           return item.withActions(createEnrollmentActions(moodleUserId, currentRolesInOodiWithDefaultRole));
       }
    }
}
