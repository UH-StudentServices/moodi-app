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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Uses a mocked CourseRepository instead of the DB initialized by Flyway.
 */
@TestPropertySource(properties = "SisuGraphQLClient.batchsize=2")
public class FullSyncJobSisuTest extends AbstractMoodiIntegrationTest {

    private static final int MOODLE_COURSE_ID_1 = 54321;
    private static final int MOODLE_COURSE_ID_2 = 54322;
    private static final int MOODLE_COURSE_ID_ENDED = 54323;
    private static final int MOODLE_COURSE_ID_ARCHIVED = 54324;

    @Autowired
    private FullSynchronizationJob job;

    @Autowired
    protected MapperService mapperService;

    @MockBean
    private CourseRepository courseRepository;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
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

        setupMoodleGetCourseResponse(MOODLE_COURSE_ID_1);
        // Two users are already enrolled with student and synced roles.
        // One of them is not enrolled for the course, and the other not in Sisu at all.
        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID_1,
            getMoodleUserEnrollments((int) MOODLE_USER_NOT_ENROLLED, MOODLE_USERNAME_NOT_ENROLLED, MOODLE_COURSE_ID_1,
                mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()),
            getMoodleUserEnrollments((int) MOODLE_USER_NOT_IN_STUDY_REGISTRY, MOODLE_USERNAME_NOT_IN_STUDY_REGISTRY, MOODLE_COURSE_ID_1,
                mapperService.getStudentRoleId(), mapperService.getMoodiRoleId())
        );
        setupMoodleGetCourseResponse(MOODLE_COURSE_ID_2);
        expectGetEnrollmentsRequestToMoodle(MOODLE_COURSE_ID_2);

        // Course one students and teacher are fetched from Moodle.
        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_MAKE, MOODLE_USER_ID_MAKE);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_NOT_ENROLLED, MOODLE_USER_NOT_ENROLLED);
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

        // The existing students gets suspended and student role removed, because no longer enrolled or found in Sisu.
        expectSuspendRequestToMoodle(
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_NOT_IN_STUDY_REGISTRY, MOODLE_COURSE_ID_1),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_NOT_ENROLLED, MOODLE_COURSE_ID_1));
        expectAssignRolesToMoodle(false,
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_NOT_IN_STUDY_REGISTRY, MOODLE_COURSE_ID_1),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_NOT_ENROLLED, MOODLE_COURSE_ID_1));

        // Course two student and teachers are enrolled.
        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_TEACH_TWO, MOODLE_COURSE_ID_2),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_TEACH_TWO, MOODLE_COURSE_ID_2),

            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_TEACH_THREE, MOODLE_COURSE_ID_2),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_TEACH_THREE, MOODLE_COURSE_ID_2),

            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_NIINA2, MOODLE_COURSE_ID_2),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_NIINA2, MOODLE_COURSE_ID_2)
        );

        job.execute();
    }

    // Initialize course repository with test courses.
    private void coursesInDb(Course...courses) {
        when(courseRepository.findByImportStatusInAndRemovedFalse(anyList())).thenReturn(Arrays.asList(courses));
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
