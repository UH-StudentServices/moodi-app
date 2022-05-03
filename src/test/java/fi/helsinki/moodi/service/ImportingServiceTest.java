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

package fi.helsinki.moodi.service;

import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportCourseResponse;
import fi.helsinki.moodi.service.importing.ImportingService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class ImportingServiceTest extends AbstractMoodiIntegrationTest {

    // add random 0-1000 millisecond delay to some moodle/sisu mock calls
    private final boolean delayed = false;

    private static final String REALISATION_ID = "hy-CUR-1";
    protected static final String CREATOR_SISU_ID = "hy-hlo-creator";
    protected static final long MOODLE_COURSE_ID = 101L;
    private static final long STUDENT_ROLE_ID = 5;
    private static final long TEACHER_ROLE_ID = 3;
    private static final long SYNCED_ROLE_ID = 10;

    private final Logger logger = LoggerFactory.getLogger(ImportingServiceTest.class);

    @Autowired
    private ImportingService importingService;

    @MockBean
    private CourseRepository courseRepository;

    @Test
    public void thatImportingServiceProcessesEnrollmentsAndReturnsRequestWithMoodleId() {
        ImportCourseRequest importCourseRequest = new ImportCourseRequest(REALISATION_ID);
        importCourseRequest.creatorSisuId = CREATOR_SISU_ID;
        AtomicReference<Course> createdCourse = new AtomicReference<>();
        when(courseRepository.save(any(Course.class)))
            .thenAnswer((Answer<Course>) invocation -> {
                Course course = (Course) invocation.getArguments()[0];
                course.id = 1001L;
                createdCourse.set(course);
                return course;
            });
        // first time this is called return empty, next time return the course saved ^ above.
        when(courseRepository.findByRealisationId(REALISATION_ID))
            .thenReturn(Optional.empty())
            .thenAnswer((Answer<Optional<Course>>) invocation -> Optional.of(createdCourse.get()));

        mockSisuGraphQLServer.expectPersonsRequest(singletonList(CREATOR_SISU_ID),
            "/sisu/persons-hy-hlo-creator.json");
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(singletonList(REALISATION_ID),
            "/sisu/course-unit-realisations-1.json");
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList("hy-hlo-1", "hy-hlo-2", "hy-hlo-2.1"),
            "/sisu/persons-many-1.json");
        expectCreateCourseRequestToMoodle(REALISATION_ID, MOODLE_COURSE_ID);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA, delayed);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA, delayed);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_MAKE, MOODLE_USER_ID_MAKE, delayed);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_ONE, MOODLE_USER_TEACH_ONE, delayed);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_CREATOR, MOODLE_USER_CREATOR, delayed);
        expectPostEnrollmentsRequestToMoodle();

        Result<ImportCourseResponse, String> result = importingService.importCourse(importCourseRequest);

        assertTrue(result.data.isPresent());
        assertEquals(MOODLE_COURSE_ID, result.data.get().moodleCourseId);
    }

    private MoodleEnrollment teacherEnrollment(long moodleUserId) {
        return new MoodleEnrollment(TEACHER_ROLE_ID, moodleUserId, MOODLE_COURSE_ID);
    }

    private MoodleEnrollment studentEnrollment(long moodleUserId) {
        return new MoodleEnrollment(STUDENT_ROLE_ID, moodleUserId, MOODLE_COURSE_ID);
    }

    private MoodleEnrollment syncEnrollment(long moodleUserId) {
        return new MoodleEnrollment(SYNCED_ROLE_ID, moodleUserId, MOODLE_COURSE_ID);
    }

    private void expectPostEnrollmentsRequestToMoodle() {
        MoodleEnrollment[] enrollments = {
            studentEnrollment(MOODLE_USER_ID_NIINA),
            syncEnrollment(MOODLE_USER_ID_NIINA),
            studentEnrollment(MOODLE_USER_ID_JUKKA),
            syncEnrollment(MOODLE_USER_ID_JUKKA),
            studentEnrollment(MOODLE_USER_ID_MAKE),
            syncEnrollment(MOODLE_USER_ID_MAKE),
            teacherEnrollment(MOODLE_USER_TEACH_ONE),
            syncEnrollment(MOODLE_USER_TEACH_ONE),
            teacherEnrollment(MOODLE_USER_CREATOR),
            syncEnrollment(MOODLE_USER_CREATOR)
        };
        expectEnrollmentRequestToMoodleWithResponse(EMPTY_RESPONSE, enrollments);
    }

    private void expectCreateCourseRequestToMoodle(final String realisationId, final long moodleCourseIdToReturn) {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andExpect(content().string(startsWith(
                        "wstoken=xxxx1234&wsfunction=core_course_create_courses&moodlewsrestformat=json&courses%5B0%5D%5Bidnumber%5D="
                        + realisationId + "&courses")))
            .andRespond(withSuccess("[{\"id\":\"" + moodleCourseIdToReturn + "\", \"shortname\":\"shortie\"}]", MediaType.APPLICATION_JSON));
    }
}
