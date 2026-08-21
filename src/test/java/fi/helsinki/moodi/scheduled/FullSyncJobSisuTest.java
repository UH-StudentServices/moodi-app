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
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Uses a mocked CourseRepository instead of the DB initialized by Flyway.
 */
@TestPropertySource(properties = { "SisuGraphQLClient.batchsize=2", "MoodleClient.batchsize=2" })
public class FullSyncJobSisuTest extends AbstractMoodiIntegrationTest {

    private static final long MOODLE_COURSE_ID_1 = 654321;
    private static final long MOODLE_COURSE_ID_2 = 654322;
    private static final long MOODLE_COURSE_ID_ENDED = 654323;
    private static final long MOODLE_COURSE_ID_ARCHIVED = 654324;

    @Autowired
    private FullSynchronizationJob job;

    @Autowired
    protected MapperService mapperService;

    @MockitoBean
    private CourseRepository courseRepository;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void thatSisuCoursesAreSynced() {
        coursesInDb(
            getCourse("hy-CUR-1", MOODLE_COURSE_ID_1),
            getCourse("hy-CUR-2", MOODLE_COURSE_ID_2),
            getCourse("hy-CUR-ended", MOODLE_COURSE_ID_ENDED),
            getCourse("hy-CUR-archived", MOODLE_COURSE_ID_ARCHIVED)
        );

        // The JSON mock data files contain the courses above.
        setUpMockSisuAndPrefetchCourses();

        // Moodle prefetching doesn't include courses that Sisu counts as finished.
        // The Moodle enrollment state itself is not fetched in delta-based synchronization.
        prepareMoodleGetCoursesResponseMock(Arrays.asList(MOODLE_COURSE_ID_1, MOODLE_COURSE_ID_2), true);

        // Course one students and teacher are fetched from Moodle.
        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_MAKE, MOODLE_USER_ID_MAKE);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_ONE, MOODLE_USER_TEACH_ONE);

        // Course two student and teachers are fetched from Moodle.
        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA2, MOODLE_USER_NIINA2);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_TWO, MOODLE_USER_TEACH_TWO);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_THREE, MOODLE_USER_TEACH_THREE);

        // Course one students and teacher are enrolled, except for the not enrolled student.
        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_1),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_1),

            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID_1),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID_1),

            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_MAKE, MOODLE_COURSE_ID_1),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID_MAKE, MOODLE_COURSE_ID_1),

            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_TEACH_ONE, MOODLE_COURSE_ID_1),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_TEACH_ONE, MOODLE_COURSE_ID_1)
        );

        // The non-enrolled Sisu student and the Moodle-only users generate no actions:
        // without a snapshot row, Moodi has never synced them, so they are left untouched.

        // Course two student and teachers are enrolled.
        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_NIINA2, MOODLE_COURSE_ID_2),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_NIINA2, MOODLE_COURSE_ID_2),

            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_TEACH_TWO, MOODLE_COURSE_ID_2),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_TEACH_TWO, MOODLE_COURSE_ID_2),

            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_TEACH_THREE, MOODLE_COURSE_ID_2),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_TEACH_THREE, MOODLE_COURSE_ID_2)
        );

        job.execute();
    }

    // Initialize course repository with test courses.
    private void coursesInDb(Course... courses) {
        when(courseRepository.findByImportStatusInAndRemovedFalseAndMoodleIdNotNull(anyList())).thenReturn(Arrays.asList(courses));
    }

    private Course getCourse(String id, long moodleId) {
        Course c = new Course();
        c.realisationId = id;
        c.moodleId = moodleId;
        c.created = LocalDateTime.MIN;
        c.modified = LocalDateTime.MIN;
        c.importStatus = Course.ImportStatus.COMPLETED;
        return c;
    }
}
