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
import com.google.common.collect.Maps;
import fi.helsinki.moodi.integration.moodle.MoodleCourseData;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.synchronize.process.UserSynchronizationActionType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserSynchronizationActionResolverTest extends AbstractMoodiIntegrationTest {

    private static final Long MOODLE_USER_ID = 1L;
    private static final Long MOODLE_COURSE_ID = 2L;
    private static final Long TEACHER_ROLE = 3L;
    private static final Long STUDENT_ROLE = 5L;
    private static final Long SYNCED_ROLE = 10L;

    @Autowired
    private UserSynchronizationActionResolver userSynchronizationActionResolver;

    @Test
    public void thatAddEnrollmentActionIsResolvedForEnrolledStudent() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(STUDENT_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatNoAddEnrollmentActionForMoodiRoleIsResolvedForNonEnrolledStudent() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(false);

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of());
    }

    @Test
    public void thatAddEnrollmentActionIsResolvedForTeacher() {
        UserSynchronizationItem item = getTeacherUserSynchronizationItem();

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(TEACHER_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatAddEnrollmentActionIsResolvedForHybridUser() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.setTeacher(getTeacher());

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(TEACHER_ROLE, STUDENT_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatSuspendEnrollmentActionIsResolvedIfAlreadyInDefaultRoleAndNotEnrolled() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(false);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(SYNCED_ROLE), MOODLE_COURSE_ID));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(SUSPEND_ENROLLMENT, newArrayList(SYNCED_ROLE)));
    }

    @Test
    public void thatAddTeacherRoleActionIsResolvedIfAlreadyInDefaultRole() {
        UserSynchronizationItem item = getTeacherUserSynchronizationItem();
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(SYNCED_ROLE)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ROLES, newArrayList(TEACHER_ROLE)));
    }

    @Test
    public void thatAddDefaultRoleActionIsResolvedForStudentIfEnrolledInMoodleWithoutDefaultRole() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(STUDENT_ROLE)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ROLES, newArrayList(SYNCED_ROLE)));
    }

    @Test
    public void thatAddDefaultRoleActionIsResolvedForTeacherIfEnrolledInMoodleWithoutDefaultRole() {
        UserSynchronizationItem item = getTeacherUserSynchronizationItem();
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(TEACHER_ROLE)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ROLES, newArrayList(SYNCED_ROLE)));
    }

    @Test
    public void thatStudentRoleIsRemoved() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(false);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(TEACHER_ROLE, STUDENT_ROLE, SYNCED_ROLE)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(REMOVE_ROLES, newArrayList(STUDENT_ROLE)));
    }

    @Test
    public void thatStudentIsSuspendedAndStudentRoleIsRemoved() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(false);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(STUDENT_ROLE, SYNCED_ROLE), MOODLE_COURSE_ID));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(
                SUSPEND_ENROLLMENT, newArrayList(SYNCED_ROLE),
                REMOVE_ROLES, newArrayList(STUDENT_ROLE)));
    }

    @Test
    public void thatStudentIsReactivatedAndStudentRoleIsAdded() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(SYNCED_ROLE)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(
                REACTIVATE_ENROLLMENT, newArrayList(STUDENT_ROLE),
                ADD_ROLES, newArrayList(STUDENT_ROLE)));
    }

    @Test
    public void thatTeacherRoleIsNeverRemoved() {
        UserSynchronizationItem item = getStudentUserSynchronizationItem(true);
        item.withMoodleUserEnrollments(getMoodleUserEnrollments(newArrayList(TEACHER_ROLE, STUDENT_ROLE, SYNCED_ROLE)));

        userSynchronizationActionResolver.enrichWithActions(item);

        assertActions(item, Maps.newHashMap());
    }

    private MoodleUserEnrollments getMoodleUserEnrollments(List<Long> roleIds, Long...moodleCourseIds) {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.roles = roleIds.stream().map(role -> {
            MoodleRole moodleRole = new MoodleRole();
            moodleRole.roleId = role;
            return moodleRole;
        }).collect(Collectors.toList());
        moodleUserEnrollments.enrolledCourses = Arrays.stream(moodleCourseIds).map(MoodleCourseData::new
            ).collect(Collectors.toList());
        return moodleUserEnrollments;
    }

    private void assertActions(UserSynchronizationItem item, Map<UserSynchronizationActionType, List<Long>> expectedActionsForRoles) {
        List<UserSynchronizationAction> actions = item.getActions();

        Set<UserSynchronizationActionType> expectedActions = expectedActionsForRoles.keySet();

        assertEquals(expectedActions.size(), actions.size());

        for (UserSynchronizationActionType actionType : expectedActions) {
            UserSynchronizationAction action = findActionByType(actionType, actions);
            Set<Long> roles = action.getRoles();
            List<Long> expectedRoles = expectedActionsForRoles.get(actionType);
            assertEquals(roles.size(), expectedRoles.size());
            assertTrue(roles.containsAll(expectedRoles));
        }
    }

    private UserSynchronizationAction findActionByType(UserSynchronizationActionType actionType, List<UserSynchronizationAction> actions) {
        return actions.stream().filter(a -> actionType.equals(a.getActionType())).findFirst().orElse(null);
    }

    private UserSynchronizationItem getStudentUserSynchronizationItem(boolean enrolled) {
        UserSynchronizationItem item = new UserSynchronizationItem(getStudent(enrolled));
        item.withMoodleUser(getMoodleUser());
        item.withMoodleCourseId(MOODLE_COURSE_ID);
        return item;
    }

    private UserSynchronizationItem getTeacherUserSynchronizationItem() {
        UserSynchronizationItem item = new UserSynchronizationItem(getTeacher());
        item.withMoodleUser(getMoodleUser());
        return item;
    }

    private StudyRegistryStudent getStudent(boolean enrolled) {
        StudyRegistryStudent student = new StudyRegistryStudent();
        student.isEnrolled = enrolled;
        return student;
    }

    private StudyRegistryTeacher getTeacher() {
        return new StudyRegistryTeacher();
    }

    private MoodleUser getMoodleUser() {
        MoodleUser moodleUser = new MoodleUser();
        moodleUser.id = MOODLE_USER_ID;
        return moodleUser;
    }
}
