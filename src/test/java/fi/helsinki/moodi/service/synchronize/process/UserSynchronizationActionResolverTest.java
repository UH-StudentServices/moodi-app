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

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.synchronize.process.UserSynchronizationActionType.ADD_ENROLLMENT;
import static fi.helsinki.moodi.service.synchronize.process.UserSynchronizationActionType.ADD_ROLES;
import static fi.helsinki.moodi.service.synchronize.process.UserSynchronizationActionType.REMOVE_ROLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserSynchronizationActionResolverTest extends AbstractMoodiIntegrationTest {

    private static final Long MOODLE_USER_ID = 1L;

    @Autowired
    private UserSynchronizationActionResolver userSynchronizationActionResolver;

    @Test
    public void thatAddEnrollmentActionIsResolvedForApprovedStudent() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(5L, 11L)));
    }

    @Test
    public void thatAddEnrollmentActionForMoodiRoleIsResolvedForUnApprovedStudent() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(false);

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(11L)));
    }

    @Test
    public void thatAddEnrollmentActionIsResolvedForTeacher() {
        UserSynchronizationItem item = getTeacherUserSynchronizationItem();

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(3L, 11L)));
    }

    @Test
    public void thatAddEnrollmentActionIsResolvedForHybridUser() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.setOodiTeacher(getOodiTeacher());

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(3L, 5L, 11L)));
    }

    @Test
    public void thatAddStudentRoleActionIsResolvedIfAlreadyInDefaultRole() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(11L)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ROLES, newArrayList(5L)));
    }

    @Test
    public void thatAddStudentRoleActionIsNotResolvedIfAlreadyInDefaultRoleAndNotApproved() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(false);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(11L)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, new HashMap<>());
    }

    @Test
    public void thatAddTeacherRoleActionIsResolvedIfAlreadyInDefaultRole() {
        UserSynchronizationItem item = getTeacherUserSynchronizationItem();
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(11L)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ROLES, newArrayList(3L)));
    }

    @Test
    public void thatAddDefaultRoleActionIsResolvedForStudentIfEnrolledInMoodleWithoutDefaultRole() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(5L)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ROLES, newArrayList(11L)));
    }

    @Test
    public void thatAddDefaultRoleActionIsResolvedForTeacherIfEnrolledInMoodleWithoutDefaultRole() {
        UserSynchronizationItem item = getTeacherUserSynchronizationItem();
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(3L)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ROLES, newArrayList(11L)));
    }

    @Test
    public void thatStudentRoleIsRemoved() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(false);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(5L, 11L)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(REMOVE_ROLES, newArrayList(5L)));
    }

    @Test
    public void thatTeacherRoleIsNeverRemoved() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(3L, 5L, 11L)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, new HashMap<>());
    }

    private MoodleUserEnrollments getMoodleUserEnrollments(List<Long> roles) {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.roles = roles.stream().map(role -> {
            MoodleRole moodleRole = new MoodleRole();
            moodleRole.roleId = role;
            return moodleRole;
        }).collect(Collectors.toList());
        return moodleUserEnrollments;
    }

    private void assertActions(UserSynchronizationItem item, Map<UserSynchronizationActionType, List<Long>> expectedActionsForRoles) {
        List<UserSynchronizationAction> actions = item.getActions();

        Set<UserSynchronizationActionType> expectedActions = expectedActionsForRoles.keySet();

        assertEquals(expectedActions.size(), actions.size());

        for (UserSynchronizationActionType actionType : expectedActions) {
            UserSynchronizationAction action = findActionByType(actionType, actions);
            List<Long> roles = action.getRoles();
            List<Long> expectedRoles = expectedActionsForRoles.get(actionType);
            assertEquals(roles.size(), expectedRoles.size());
            assertTrue(roles.containsAll(expectedRoles));
        }
    }

    private UserSynchronizationAction findActionByType(UserSynchronizationActionType actionType, List<UserSynchronizationAction> actions) {
        return actions.stream().filter(a -> actionType.equals(a.getActionType())).findFirst().orElse(null);
    }

    private UserSynchronizationItem getStudentUserSynchronizationItem(boolean approved) {
        UserSynchronizationItem item = new UserSynchronizationItem(getOodiStudent(approved));
        item.withMoodleUser(getMoodleUser());
        return item;
    }

    private UserSynchronizationItem getTeacherUserSynchronizationItem() {
        UserSynchronizationItem item = new UserSynchronizationItem(new OodiTeacher());
        item.withMoodleUser(getMoodleUser());
        return item;
    }

    private OodiStudent getOodiStudent(boolean approved) {
        OodiStudent student = new OodiStudent();
        student.approved = approved;
        return student;
    }

    private OodiTeacher getOodiTeacher() {
        return new OodiTeacher();
    }

    private MoodleUser getMoodleUser() {
        MoodleUser moodleUser = new MoodleUser();
        moodleUser.id = MOODLE_USER_ID;
        return moodleUser;
    }
}
