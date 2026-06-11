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

package fi.helsinki.moodi.service.synchronize.snapshot;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class CourseSyncParticipantServiceTest extends AbstractMoodiIntegrationTest {

    private static final String USERNAME_1 = "user1@helsinki.fi";
    private static final String USERNAME_2 = "user2@helsinki.fi";

    @Autowired
    private CourseSyncParticipantService courseSyncParticipantService;

    @Autowired
    private CourseService courseService;

    private Long courseId() {
        return courseService.findByRealisationId(SISU_REALISATION_IN_DB_ID).get().id;
    }

    @Test
    public void thatSnapshotRowsAreInserted() {
        Long courseId = courseId();
        courseSyncParticipantService.upsertAll(courseId, Arrays.asList(
            new CourseSyncParticipant(courseId, USERNAME_1, 1L, true, true, false),
            new CourseSyncParticipant(courseId, USERNAME_2, 2L, false, false, true)));

        Map<String, CourseSyncParticipant> snapshot = courseSyncParticipantService.getSnapshotByUsername(courseId);

        assertEquals(2, snapshot.size());
        assertTrue(snapshot.get(USERNAME_1).enrolledStudent);
        assertTrue(snapshot.get(USERNAME_2).teacher);
        assertNotNull(snapshot.get(USERNAME_1).created);
        assertNotNull(snapshot.get(USERNAME_1).modified);
    }

    @Test
    public void thatExistingSnapshotRowIsUpdated() {
        Long courseId = courseId();
        courseSyncParticipantService.upsertAll(courseId, Collections.singletonList(
            new CourseSyncParticipant(courseId, USERNAME_1, 1L, true, true, false)));

        courseSyncParticipantService.upsertAll(courseId, Collections.singletonList(
            new CourseSyncParticipant(courseId, USERNAME_1, null, true, false, false)));

        Map<String, CourseSyncParticipant> snapshot = courseSyncParticipantService.getSnapshotByUsername(courseId);

        assertEquals(1, snapshot.size());
        CourseSyncParticipant row = snapshot.get(USERNAME_1);
        assertFalse(row.enrolledStudent);
        // A null moodleUserId in the update does not clear the cached id.
        assertEquals((Long) 1L, row.moodleUserId);
    }

    @Test
    public void thatSnapshotRowsAreDeletedByCourseId() {
        Long courseId = courseId();
        courseSyncParticipantService.upsertAll(courseId, Collections.singletonList(
            new CourseSyncParticipant(courseId, USERNAME_1, 1L, true, true, false)));

        courseSyncParticipantService.deleteByCourseId(courseId);

        assertTrue(courseSyncParticipantService.getSnapshotByUsername(courseId).isEmpty());
    }

    @Test
    public void thatSnapshotRowsAreDeletedWhenCourseIsDeleted() {
        Long courseId = courseId();
        courseSyncParticipantService.upsertAll(courseId, Collections.singletonList(
            new CourseSyncParticipant(courseId, USERNAME_1, 1L, true, true, false)));

        Course course = courseService.findByRealisationId(SISU_REALISATION_IN_DB_ID).get();
        courseService.deleteCourse(course.id);

        assertTrue(courseSyncParticipantService.getSnapshotByUsername(courseId).isEmpty());
    }
}
