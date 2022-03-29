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

package fi.helsinki.moodi.moodle;

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.Application;
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportingService;
import fi.helsinki.moodi.service.importing.MoodleCourseBuilder;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.MockSisuGraphQLServer;
import fi.helsinki.moodi.test.TestConfig;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static java.lang.Math.abs;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    properties = { "server.port:0", "test.MoodleCourseBuilder.overrideShortname:true" },
    classes = { Application.class, TestConfig.class })
public abstract class AbstractMoodleIntegrationTest {

    @Autowired
    private ImportingService importingService;

    @Autowired
    protected MoodleClient moodleClient;

    @Autowired
    protected MapperService mapperService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    protected SynchronizationService synchronizationService;

    @Autowired
    protected MoodleCourseBuilder moodleCourseBuilder;

    @Autowired
    protected RestTemplate studyRegistryRestTemplate;

    @Autowired
    protected Environment environment;

    @Autowired
    private CacheManager cacheManager;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, 9876);
    // Gets populated by the @Rule above.
    private MockServerClient mockServerClient;
    protected MockSisuGraphQLServer mockSisuGraphQLServer;
    protected MockRestServiceServer studyRegistryMockServer;

    protected static final String STUDENT_USERNAME = "doo_2@helsinki.fi";
    protected static final String STUDENT_NOT_IN_MOODLE_USERNAME = "username_of_student_not_in_moodle";
    protected static final String TEACHER_USERNAME = "doo_1@helsinki.fi";
    protected static final String CREATOR_USERNAME = "doo_7@helsinki.fi";

    protected final StudentUser studentUser = new StudentUser(STUDENT_USERNAME, "014010293", true);
    protected final TeacherUser studentUserInTeacherRole = new TeacherUser(STUDENT_USERNAME, "hy-hlo-student-1");
    protected final StudentUser studentUserNotInMoodle = new StudentUser(STUDENT_NOT_IN_MOODLE_USERNAME, "012345678", true);
    protected final TeacherUser teacherUser = new TeacherUser(TEACHER_USERNAME, "hy-hlo-teacher-1");
    protected final StudentUser teacherInStudentRole = new StudentUser(TEACHER_USERNAME, "011911609", true);

    protected final TeacherUser creatorUser = new TeacherUser(CREATOR_USERNAME, "hy-hlo-creator-1");

    @Before
    public void emptyCoursesAndClearCaches() {
        courseRepository.deleteAll();
        cacheManager.getCacheNames().stream().forEach(c -> cacheManager.getCache(c).clear());

    }

    @Before
    public final void setUpMockServers() {
        mockSisuGraphQLServer = new MockSisuGraphQLServer(mockServerClient);
        studyRegistryMockServer = MockRestServiceServer.createServer(studyRegistryRestTemplate);
        expectSisuOrganisationsRequest();
    }

    @After
    public void verifyMockServers() {
        studyRegistryMockServer.verify();
        mockSisuGraphQLServer.verify();
    }

    protected String getSisuCourseId() {
        return "hy-CUR-" + abs(new Random().nextInt());
    }

    protected long importCourse(String courseId) {
        return importCourse(courseId, null);
    }

    protected long importCourse(String courseId, String creatorId) {
        ImportCourseRequest importCourseRequest = new ImportCourseRequest();
        importCourseRequest.realisationId = courseId;
        importCourseRequest.creatorSisuId = creatorId;

        return importingService.importCourse(importCourseRequest).data
            .map(c -> c.moodleCourseId).orElseThrow(() -> new RuntimeException("Course import failed"));
    }

    protected MoodleUserEnrollments findEnrollmentsByUsername(List<MoodleUserEnrollments> moodleUserEnrollmentsList, String username) {
        return moodleUserEnrollmentsList.stream()
            .filter(e -> username.equals(e.username))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Enrollment not found for " + username));
    }

    protected static class StudentUser extends IntegrationTestUser {
        public String studentNumber;
        public boolean enrolled;

        public StudentUser(String username, String studentNumber, boolean enrolled) {
            super(username);
            this.studentNumber = studentNumber;
            this.enrolled = enrolled;
        }

        public StudentUser setEnrolled(boolean enrolled) {
            return new StudentUser(this.username, this.studentNumber, enrolled);
        }

        // Referred to by course-unit-realisations-itest.json
        public String getState() {
            return this.enrolled ? "ENROLLED" : "REJECTED";
        }
    }

    protected static class TeacherUser extends IntegrationTestUser {
        public String personId;
        public String roleUrn = "urn:code:course-unit-realisation-responsibility-info-type:responsible-teacher";

        public TeacherUser(String username, String personId) {
            super(username);
            this.personId = personId;
        }
    }

    protected abstract static class IntegrationTestUser {
        public String username;

        public IntegrationTestUser(String username) {
            this.username = username;
        }
    }

    private Map curVariables(String courseId, List<StudentUser> students, List<TeacherUser> teachers) {
        return curVariables(courseId, students, teachers, "urn:code:language:fi");
    }

    @SuppressWarnings("unchecked")
    private Map curVariables(String courseId, List<StudentUser> students, List<TeacherUser> teachers, String teachingLanguage) {
        return new ImmutableMap.Builder()
            .put("courseId", courseId)
            .put("students", students)
            .put("teachers", teachers)
            .put("endDate", getFutureDateString())
            .put("teachingLanguage", teachingLanguage)
            .build();
    }

    private void expectTeachers(List<TeacherUser> teachers) {
        mockSisuGraphQLServer.expectPersonsRequest(teachers.stream().map(t -> t.personId).collect(Collectors.toList()),
            "/sisu-itest/persons-itest.json",
            new ImmutableMap.Builder<String, List<TeacherUser>>()
                .put("teachers", teachers).build());
    }

    protected void expectCreator(TeacherUser creator) {
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList(creator.personId),
            "/sisu-itest/persons-itest.json",
            new ImmutableMap.Builder<String, List<TeacherUser>>()
                .put("teachers", Arrays.asList(creator)).build());
    }

    protected void expectCourseRealisationsWithUsers(String courseId, List<StudentUser> students, List<TeacherUser> teachers) {
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(Arrays.asList(courseId),
            "/sisu-itest/course-unit-realisations-itest.json",

            curVariables(courseId, students, teachers));

        if (!teachers.isEmpty()) {
            expectTeachers(teachers);
        }
    }

    protected void expectSisuOrganisationsRequest() {
        studyRegistryMockServer.expect(requestTo(environment.getProperty("integration.sisu.baseUrl") +
                "/kori/api/organisations/v2/export?limit=10000&since=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Fixtures.asString("/sisu/organisation-export.json"), MediaType.APPLICATION_JSON));
    }

    protected void resetAndExpectCourseRealisationsWithUsers(String courseId, List<StudentUser> students, List<TeacherUser> teachers) {
        mockSisuGraphQLServer.reset();
        expectCourseRealisationsWithUsers(courseId, students, teachers);
    }

    protected void assertUserEnrollments(String username,
                                         List<MoodleUserEnrollments> moodleUserEnrollmentsList,
                                         List<Long> expectedRoleIds) {
        MoodleUserEnrollments userEnrollments = findEnrollmentsByUsername(moodleUserEnrollmentsList, username);

        assertEquals(expectedRoleIds.size(), userEnrollments.roles.size());

        expectedRoleIds.stream().forEach(roleId -> assertTrue(userEnrollments.hasRole(roleId)));
    }

    protected void assertStudentEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            newArrayList(mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()));
    }

    protected void assertUserCourseVisibility(boolean expectedVisibility,
                                              String username,
                                              Long courseId,
                                              List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertEquals(expectedVisibility, findEnrollmentsByUsername(moodleUserEnrollmentsList, username).seesCourse(courseId));
    }

    protected void assertTeacherEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            newArrayList(mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()));
    }

    protected void assertHybridEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            newArrayList(mapperService.getStudentRoleId(), mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()));
    }

    /*
        Every user is that has been enrolled or updated by moodi is tagged with "moodi role".
        This role is never removed, even if other roles are removed. Note that only student role can be removed by Moodi,
        if student is returned from Sisu with approved set to false, or is not returned at in the enrollments list at all.
     */
    protected void assertMoodiRoleEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            singletonList(mapperService.getMoodiRoleId()));
    }
}
