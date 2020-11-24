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

package fi.helsinki.moodi.service.log;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.log.SynchronizationSummaryLog.*;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationAction;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationAction.UserSynchronizationActionStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationActionType;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem.UserSynchronizationItemStatus;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class SynchronizationSummaryLogTest extends AbstractSummaryLogTest {

    private static final long TEACHER_MOODLE_USER_ID = 1;
    private static final long STUDENT_MOODLE_USER_ID = 2;

    private static final long COURSE_MOODLE_ID = 1;
    private static final String COURSE_REALISATION_ID = "2";

    private static final String STUDENT_MOODLE_USERNAME = "studentUsername";
    private static final String TEACHER_MOODLE_USERNAME = "teacherUsername";

    private static final long STUDENT_ROLE_ID = 5;
    private static final long TEACHER_ROLE_ID = 3;

    private static final int ENROLLMENT_STATUS_CODE = 2;

    private static final String ENRICHMENT_SUCCESSFUL_MESSAGE = "Enrichment successful";

    @Test
    public void thatSynchronizationSummaryLogIsCreated() {

        SynchronizationType synchronizationType = SynchronizationType.FULL;

        SynchronizationItem item = getSynchronizationItem(synchronizationType);

        SynchronizationItem itemWithUserItems = item.setUserSynchronizationItems(newArrayList(
            getSuccessfulStudentUserSynchronizationItem(),
            getFailedTeacherUserStudentUserSynchronizationItem()));

        SynchronizationItem completedItem = itemWithUserItems
            .completeEnrichmentPhase(EnrichmentStatus.SUCCESS, ENRICHMENT_SUCCESSFUL_MESSAGE)
            .completeProcessingPhase();

        List<SynchronizationItem> synchronizationItems = newArrayList(completedItem);

        SynchronizationSummary summary = new SynchronizationSummary(synchronizationType, synchronizationItems, Stopwatch.createUnstarted());

        SynchronizationSummaryLog synchronizationSummaryLog = new SynchronizationSummaryLog(summary);

        SynchronizationSymmaryLogRoot logRoot = synchronizationSummaryLog.getSyncronizationSummary();

        assertEquals(synchronizationType, logRoot.type);
        assertEquals(0, logRoot.successfulItemsCount);
        assertEquals(1, logRoot.failedItemsCount);

        SynchronizationItemLogEntry synchronizationItemLogEntry = logRoot.courses.get(0);
        assertEquals(COURSE_REALISATION_ID, synchronizationItemLogEntry.realisationId);
        assertEquals(COURSE_MOODLE_ID, synchronizationItemLogEntry.moodleId);
        assertEquals(EnrichmentStatus.SUCCESS, synchronizationItemLogEntry.enrichmentStatus);
        assertEquals(ProcessingStatus.ENROLLMENT_FAILURES, synchronizationItemLogEntry.processingStatus);
        assertEquals(ENRICHMENT_SUCCESSFUL_MESSAGE, synchronizationItemLogEntry.enrichmentMessage);
        assertEquals(SynchronizationItem.ENROLLMENT_FAILURES_MESSAGE, synchronizationItemLogEntry.processingMessage);

        UserEnrollmentsLogEntry userEnrollmentsLogEntry = synchronizationItemLogEntry.userEnrollments;
        Map<UserSynchronizationItemStatus, Long> synchronizationItemLogEntrySummary = userEnrollmentsLogEntry.summary;

        assertEquals(1, synchronizationItemLogEntrySummary.get(UserSynchronizationItemStatus.SUCCESS).intValue());
        assertEquals(1, synchronizationItemLogEntrySummary.get(UserSynchronizationItemStatus.ERROR).intValue());

        Map<UserSynchronizationItemStatus, List<UserSyncronizationItemLogEntry>> synchronizationItemLogEntryResults = userEnrollmentsLogEntry.results;
        List<UserSyncronizationItemLogEntry> successfullUserEntries = synchronizationItemLogEntryResults.get(UserSynchronizationItemStatus.SUCCESS);
        List<UserSyncronizationItemLogEntry> failedUserEntries = synchronizationItemLogEntryResults.get(UserSynchronizationItemStatus.ERROR);

        assertEquals(1, successfullUserEntries.size());
        assertEquals(1, failedUserEntries.size());

        UserSyncronizationItemLogEntry successfulEntry = successfullUserEntries.get(0);

        assertTrue(successfulEntry.status.equals(UserSynchronizationItemStatus.SUCCESS));
        assertTrue(successfulEntry.moodleUserId.equals(STUDENT_MOODLE_USER_ID));
        assertEquals(STUDENT_NUMBER, successfulEntry.oodiStudent.studentNumber);
        assertEquals(ENROLLMENT_STATUS_CODE, successfulEntry.oodiStudent.enrollmentStatusCode);
        assertTrue(successfulEntry.oodiStudent.approved);
        assertFalse(successfulEntry.oodiStudent.automaticEnabled);
        assertNull(successfulEntry.oodiTeacher);
        assertSingleAction(
            successfulEntry.actions,
            UserSynchronizationActionStatus.SUCCESS,
            UserSynchronizationActionType.REMOVE_ROLES,
            newArrayList(STUDENT_ROLE_ID));
        assertEquals(STUDENT_MOODLE_USERNAME, successfulEntry.moodleUsername);
        assertEquals(singletonList(STUDENT_ROLE_ID), successfulEntry.moodleRoleIds);

        UserSyncronizationItemLogEntry failedEntry = failedUserEntries.get(0);
        assertTrue(failedEntry.status.equals(UserSynchronizationItemStatus.ERROR));
        assertTrue(failedEntry.moodleUserId.equals(TEACHER_MOODLE_USER_ID));
        assertNull(failedEntry.oodiStudent);
        assertEquals(TEACHER_ID, failedEntry.oodiTeacher.teacherId);
        assertSingleAction(
            failedEntry.actions,
            UserSynchronizationActionStatus.ERROR,
            UserSynchronizationActionType.ADD_ROLES,
            newArrayList(TEACHER_ROLE_ID));
        assertEquals(TEACHER_MOODLE_USERNAME, failedEntry.moodleUsername);
        assertEquals(emptyList(), failedEntry.moodleRoleIds);
    }

    private void assertSingleAction(List<SyncronizationItemActionLogEntry> actions,
                                    UserSynchronizationActionStatus expectedStatus,
                                    UserSynchronizationActionType expectedActionType,
                                    List<Long> expectedRoles) {

        assertEquals(1, actions.size());

        SyncronizationItemActionLogEntry action = actions.get(0);
        assertEquals(expectedStatus, action.status);
        assertEquals(expectedActionType, action.actionType);
        assertEquals(action.roles.size(), expectedRoles.size());
        assertTrue(action.roles.containsAll(expectedRoles));

    }

    private SynchronizationItem getSynchronizationItem(SynchronizationType synchronizationType) {
        Course course = new Course();
        course.moodleId = COURSE_MOODLE_ID;
        course.realisationId = COURSE_REALISATION_ID;

        return new SynchronizationItem(course, synchronizationType);
    }

    private UserSynchronizationItem getSuccessfulStudentUserSynchronizationItem() {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        MoodleRole moodleRole = new MoodleRole();
        moodleRole.roleId = STUDENT_ROLE_ID;
        moodleUserEnrollments.username = STUDENT_MOODLE_USERNAME;
        moodleUserEnrollments.roles = singletonList(moodleRole);
        UserSynchronizationItem item = new UserSynchronizationItem(getOodiStudent());

        return enrichUserSynchronizationItem(
            item,
            UserSynchronizationItemStatus.SUCCESS,
            STUDENT_MOODLE_USER_ID,
            newArrayList(new UserSynchronizationAction(UserSynchronizationActionType.REMOVE_ROLES,
                    Sets.newHashSet(STUDENT_ROLE_ID), STUDENT_MOODLE_USER_ID)
                .withSuccessStatus()),
            moodleUserEnrollments);
    }

    private UserSynchronizationItem getFailedTeacherUserStudentUserSynchronizationItem() {
        UserSynchronizationItem item = new UserSynchronizationItem(getOodiTeacher());

        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.username = TEACHER_MOODLE_USERNAME;

        return enrichUserSynchronizationItem(
                item,
                UserSynchronizationItemStatus.ERROR,
                TEACHER_MOODLE_USER_ID,
                newArrayList(new UserSynchronizationAction(UserSynchronizationActionType.ADD_ROLES,
                        Sets.newHashSet(TEACHER_ROLE_ID),
                        TEACHER_MOODLE_USER_ID)
                        .withErrorStatus()),
                moodleUserEnrollments);
    }

    private UserSynchronizationItem enrichUserSynchronizationItem(UserSynchronizationItem item,
                                                                  UserSynchronizationItemStatus status,
                                                                  Long moodleUserId,
                                                                  List<UserSynchronizationAction> actions,
                                                                  MoodleUserEnrollments moodleUserEnrollments) {
        MoodleUser moodleUser = new MoodleUser();
        moodleUser.id = moodleUserId;

        return item
            .withMoodleUser(moodleUser)
            .withStatus(status)
            .withActions(actions)
            .withMoodleUserEnrollments(moodleUserEnrollments);
    }

    private OodiStudent getOodiStudent() {
        OodiStudent student = new OodiStudent();
        student.studentNumber = STUDENT_NUMBER;
        student.approved = true;
        student.automaticEnabled = false;
        student.enrollmentStatusCode = ENROLLMENT_STATUS_CODE;
        return student;
    }

    private OodiTeacher getOodiTeacher() {
        OodiTeacher teacher = new OodiTeacher();
        teacher.teacherId = TEACHER_ID;
        return teacher;
    }
}
