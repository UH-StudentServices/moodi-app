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

package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.snapshot.CourseSyncParticipant;
import org.junit.Test;

import java.util.Collections;

import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static org.junit.Assert.*;

/**
 * RECONCILE synchronization compares against the live Moodle enrollment state (like the
 * old FULL synchronization did) and rewrites the snapshots used by delta-based runs.
 * It is used to bootstrap the snapshots and to correct drift caused by manual changes
 * in Moodle.
 */
public class ReconcileSynchronizationJobTest extends AbstractSynchronizationJobTest {

    @Test
    public void thatReconcileSynchronizesAgainstLiveMoodleStateAndWritesSnapshots() {
        setUpMockServerResponses(getFutureDateString(), true);

        // Niina is already enrolled in Moodle with the teacher role.
        prepareMoodleGetEnrolledUsersForCoursesMock(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA, (int) MOODLE_COURSE_ID_IN_DB,
                    mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodle(true, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB));

        synchronizationService.synchronize(SynchronizationType.RECONCILE);

        // The reconciled Sisu state is written to the snapshots used by delta-based runs.
        CourseSyncParticipant studentSnapshot = getSnapshot(MOODLE_USERNAME_NIINA);
        assertTrue(studentSnapshot.student);
        assertTrue(studentSnapshot.enrolledStudent);
        assertEquals((Long) MOODLE_USER_ID_NIINA, studentSnapshot.moodleUserId);

        CourseSyncParticipant teacherSnapshot = getSnapshot(MOODLE_USERNAME_HRAOPE);
        assertTrue(teacherSnapshot.teacher);
    }

    @Test
    public void thatUserWithSyncRoleIsNotSuspendedIfDoesNotSeeCourse() {
        setUpMockServerResponses(getFutureDateString(), false);

        prepareMoodleGetEnrolledUsersForCoursesMock(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA,
                    (int) SOME_OTHER_MOODLE_COURSE_ID, mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        synchronizationService.synchronize(SynchronizationType.RECONCILE);
    }

    @Test
    public void thatStaleSnapshotIsCorrectedByReconcile() {
        setUpMockServerResponses(getFutureDateString(), true);

        // The snapshot claims Niina is synced, but she is not actually enrolled in Moodle
        // (e.g. the enrollment was removed manually).
        seedSnapshot(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, true, true, false);
        seedSnapshot(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE, false, false, true);

        prepareMoodleGetEnrolledUsersForCoursesMock(MOODLE_COURSE_ID_IN_DB, Collections.emptyList());

        expectFindUsersRequestsToMoodle();

        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        synchronizationService.synchronize(SynchronizationType.RECONCILE);
    }
}
