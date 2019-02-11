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
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synclock.SyncLockService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.*;

@TestPropertySource(properties = {
    "syncTresholds.REMOVE_ROLES.preventAll = 1",
    "syncTresholds.REMOVE_ROLES.limit = 10"})
public class LockedCourseSynchronizationJobTest extends AbstractSynchronizationJobTest {

    private static final String LOCK_MESSAGE = "Locked";

    @Autowired
    private SyncLockService syncLockService;

    @Autowired
    private CourseService courseService;

    @Before
    public void init() {
        Course course = getTestCourse();
        syncLockService.setLock(course, LOCK_MESSAGE);
    }

    @Test
    public void thatSynchronizationSkipsLockedCourse() {
        SynchronizationSummary summary = synchronizationService.synchronize(SynchronizationType.FULL);

        SynchronizationItem item = summary.getItems().get(0);

        assertEquals(item.getEnrichmentStatus(), EnrichmentStatus.LOCKED);
        assertEquals(item.getProcessingStatus(), ProcessingStatus.SKIPPED);
    }

    @Test
    public void thatLockedCourseIsUnlockedWhenUnlockSynchronizationIsTriggeredAndThresholdsAreNotExceeded() {
        Course course = getTestCourse();
        assertTrue(syncLockService.isLocked(course));
        testSynchronizationSummary(SynchronizationType.UNLOCK, EMPTY_RESPONSE, false);
        assertFalse(syncLockService.isLocked(course));
    }

    @Test
    public void thatLockedCourseIsUnlockedWhenUnlockSynchronizationIsTriggeredAndThresholdsAreExceeded() {
        expectEnrollmentRequestToMoodleWithResponse(EMPTY_RESPONSE,
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        expectSuspendRequestToMoodle(new MoodleEnrollment(getMoodiRoleId(), STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID));

        expectAssignRolesToMoodleWithResponse(
            EMPTY_RESPONSE,
            false,
            new MoodleEnrollment(getStudentRoleId(),
                STUDENT_USER_MOODLE_ID,
                MOODLE_COURSE_ID)
        );

        Course course = getTestCourse();
        assertTrue(syncLockService.isLocked(course));

        testSynchronizationSummaryWhenRemovingRoles(
            SynchronizationType.UNLOCK,
            ProcessingStatus.SUCCESS,
            SynchronizationItem.SUCCESS_MESSAGE);

        assertFalse(syncLockService.isLocked(course));
    }

    private Course getTestCourse() {
        return courseService.findByRealisationId(REALISATION_ID).get();
    }

}
