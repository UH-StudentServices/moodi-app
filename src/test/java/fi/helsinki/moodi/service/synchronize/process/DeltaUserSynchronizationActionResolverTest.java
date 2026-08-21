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
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import fi.helsinki.moodi.service.synchronize.snapshot.CourseSyncParticipant;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.synchronize.process.UserSynchronizationActionType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeltaUserSynchronizationActionResolverTest extends AbstractMoodiIntegrationTest {

    private static final Long MOODLE_USER_ID = 1L;
    private static final Long COURSE_ID = 7L;
    private static final Long TEACHER_ROLE = 3L;
    private static final Long STUDENT_ROLE = 5L;
    private static final Long SYNCED_ROLE = 10L;
    private static final String USERNAME = "user@helsinki.fi";

    @Autowired
    private DeltaUserSynchronizationActionResolver resolver;

    @Test
    public void thatAddEnrollmentIsResolvedForNewEnrolledStudent() {
        UserSynchronizationItem item = studentItem(true, null);

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(STUDENT_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatNoActionIsResolvedForNewNonEnrolledStudent() {
        UserSynchronizationItem item = studentItem(false, null);

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of());
    }

    @Test
    public void thatAddEnrollmentIsResolvedForNewTeacher() {
        UserSynchronizationItem item = teacherItem(null);

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(TEACHER_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatAddEnrollmentIsResolvedForNewHybridUser() {
        UserSynchronizationItem item = studentItem(true, null);
        item.setTeacher(teacher());

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(TEACHER_ROLE, STUDENT_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatNoActionIsResolvedForUnchangedEnrolledStudent() {
        UserSynchronizationItem item = studentItem(true, snapshot(true, true, false));

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of());
    }

    @Test
    public void thatNoActionIsResolvedForUnchangedTeacher() {
        UserSynchronizationItem item = teacherItem(snapshot(false, false, true));

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of());
    }

    @Test
    public void thatSuspendIsResolvedForStudentNoLongerEnrolled() {
        UserSynchronizationItem item = studentItem(false, snapshot(true, true, false));

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(
            SUSPEND_ENROLLMENT, newArrayList(SYNCED_ROLE),
            REMOVE_ROLES, newArrayList(STUDENT_ROLE)));
    }

    @Test
    public void thatSuspendIsResolvedForStudentRemovedFromSisu() {
        UserSynchronizationItem item = new UserSynchronizationItem(snapshot(true, true, false));

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(
            SUSPEND_ENROLLMENT, newArrayList(SYNCED_ROLE),
            REMOVE_ROLES, newArrayList(STUDENT_ROLE)));
    }

    @Test
    public void thatReactivateIsResolvedForReEnrolledStudent() {
        UserSynchronizationItem item = studentItem(true, snapshot(true, false, false));

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(REACTIVATE_ENROLLMENT, newArrayList(STUDENT_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatOnlyStudentRoleIsRemovedFromTeacherNoLongerEnrolledAsStudent() {
        UserSynchronizationItem item = studentItem(false, snapshot(true, true, true));
        item.setTeacher(teacher());

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(REMOVE_ROLES, newArrayList(STUDENT_ROLE)));
    }

    @Test
    public void thatStudentRoleRemovalAndTeacherEnrollmentAreResolvedForStudentTurnedTeacher() {
        UserSynchronizationItem item = studentItem(false, snapshot(true, true, false));
        item.setTeacher(teacher());

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(
            REMOVE_ROLES, newArrayList(STUDENT_ROLE),
            ADD_ENROLLMENT, newArrayList(TEACHER_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatAddEnrollmentIsResolvedForNewTeacherRoleOfExistingStudent() {
        UserSynchronizationItem item = studentItem(true, snapshot(true, true, false));
        item.setTeacher(teacher());

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of(ADD_ENROLLMENT, newArrayList(TEACHER_ROLE, SYNCED_ROLE)));
    }

    @Test
    public void thatNoActionIsResolvedForRemovedTeacher() {
        UserSynchronizationItem item = new UserSynchronizationItem(snapshot(false, false, true));

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of());
    }

    @Test
    public void thatNoActionIsResolvedForAlreadySuspendedUserAbsentFromSisu() {
        UserSynchronizationItem item = new UserSynchronizationItem(snapshot(false, false, false));

        resolver.enrichWithActions(item);

        assertActions(item, ImmutableMap.of());
    }

    private CourseSyncParticipant snapshot(boolean student, boolean enrolledStudent, boolean teacher) {
        return new CourseSyncParticipant(COURSE_ID, USERNAME, MOODLE_USER_ID, student, enrolledStudent, teacher);
    }

    private UserSynchronizationItem studentItem(boolean enrolled, CourseSyncParticipant snapshot) {
        StudyRegistryStudent student = new StudyRegistryStudent();
        student.userName = USERNAME;
        student.isEnrolled = enrolled;
        UserSynchronizationItem item = new UserSynchronizationItem(student);
        item.withMoodleUser(moodleUser());
        return item.withExistingSnapshot(snapshot);
    }

    private UserSynchronizationItem teacherItem(CourseSyncParticipant snapshot) {
        UserSynchronizationItem item = new UserSynchronizationItem(teacher());
        item.withMoodleUser(moodleUser());
        return item.withExistingSnapshot(snapshot);
    }

    private StudyRegistryTeacher teacher() {
        StudyRegistryTeacher teacher = new StudyRegistryTeacher();
        teacher.userName = USERNAME;
        return teacher;
    }

    private MoodleUser moodleUser() {
        MoodleUser moodleUser = new MoodleUser();
        moodleUser.setId(MOODLE_USER_ID);
        return moodleUser;
    }

    private void assertActions(UserSynchronizationItem item, Map<UserSynchronizationActionType, List<Long>> expectedActionsForRoles) {
        List<UserSynchronizationAction> actions = item.getActions();

        Set<UserSynchronizationActionType> expectedActions = expectedActionsForRoles.keySet();

        assertEquals(expectedActions.size(), actions.size());

        for (UserSynchronizationActionType actionType : expectedActions) {
            UserSynchronizationAction action = actions.stream()
                .filter(a -> actionType.equals(a.getActionType()))
                .findFirst().orElse(null);
            Set<Long> roles = action.getRoles();
            List<Long> expectedRoles = expectedActionsForRoles.get(actionType);
            assertEquals(expectedRoles.size(), roles.size());
            assertTrue(roles.containsAll(expectedRoles));
        }
    }
}
