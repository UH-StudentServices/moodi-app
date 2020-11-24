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
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportingService;
import fi.helsinki.moodi.service.importing.MoodleCourseBuilder;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.iam.IAMService.DOMAIN_SUFFIX;
import static fi.helsinki.moodi.service.iam.IAMService.TEACHER_ID_PREFIX;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static java.lang.Math.abs;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public abstract class AbstractMoodleIntegrationTest extends AbstractMoodiIntegrationTest {

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

    protected static final String STUDENT_USERNAME = "doo_2";
    protected static final String STUDENT_NOT_IN_MOODLE_USERNAME = "username_of_student_not_in_moodle";
    protected static final String TEACHER_USERNAME = "doo_1";

    protected static final String INTEGRATION_TEST_OODI_FIXTURES_PREFIX = "src/itest/resources/fixtures/oodi/";

    protected final StudentUser studentUser = new StudentUser(STUDENT_USERNAME, "014010293", true);
    protected final TeacherUser studentUserInTeacherRole = new TeacherUser(STUDENT_USERNAME, "01143451");
    protected final StudentUser studentUserNotInMoodle = new StudentUser(STUDENT_NOT_IN_MOODLE_USERNAME, "012345678", true);
    protected final TeacherUser teacherUser = new TeacherUser(TEACHER_USERNAME, "011631484");
    protected final StudentUser teacherInStudentRole = new StudentUser(TEACHER_USERNAME, "011911609", true);

    @Before
    public void emptyCourses() {
        courseRepository.deleteAll();
    }

    @Before
    @Override
    public final void setUpMockServers() {
        oodiMockServer = MockRestServiceServer.createServer(oodiRestTemplate);
        iamMockServer = MockRestServiceServer.createServer(iamRestTemplate);
    }

    @After
    @Override
    public final void verifyMockServers() {
        oodiMockServer.verify();
        iamMockServer.verify();
    }

    protected String getOodiCourseId() {
        return "" + abs(new Random().nextInt());
    }

    protected long importCourse(String courseId) {
        ImportCourseRequest importCourseRequest = new ImportCourseRequest();
        importCourseRequest.realisationId = courseId;

        return importingService.importCourse(importCourseRequest).data
            .map(c -> c.moodleCourseId).orElseThrow(() -> new RuntimeException("Course import failed"));
    }

    protected MoodleUserEnrollments findEnrollmentsByUsername(List<MoodleUserEnrollments> moodleUserEnrollmentsList, String username) {
        return moodleUserEnrollmentsList.stream()
            .filter(e -> (username + DOMAIN_SUFFIX).equals(e.username))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Enrollment not found for " + username));
    }

    protected static class StudentUser extends IntegrationTestUser {
        public String studentNumber;
        public boolean approved;

        public StudentUser(String username, String studentNumber, boolean approved) {
            super(username);
            this.studentNumber = studentNumber;
            this.approved = approved;
        }

        public StudentUser setApproved(boolean newApproved) {
            return new StudentUser(this.username, this.studentNumber, newApproved);
        }
    }

    protected static class TeacherUser extends IntegrationTestUser {
        public String teacherId;

        public TeacherUser(String username, String teacherId) {
            super(username);
            this.teacherId = teacherId;
        }
    }

    protected abstract static class IntegrationTestUser {
        public String username;

        public IntegrationTestUser(String username) {
            this.username = username;
        }
    }

    protected void expectCourseUsersWithUsers(String courseId, List<StudentUser> students, List<TeacherUser> teachers) {
        expectCourseRealisationWithUsers(this::expectGetCourseUsersRequestToOodi, courseId, students, teachers);
    }

    protected void expectCourseRealisationWithUsers(String courseId, List<StudentUser> students, List<TeacherUser> teachers) {
        expectCourseRealisationWithUsers(this::expectGetCourseUnitRealisationRequestToOodi, courseId, students, teachers);
    }

    private void expectCourseRealisationWithUsers(BiConsumer<String, ResponseCreator> expectationFn, String courseId, List<StudentUser> students,
        List<TeacherUser> teachers) {
        expectationFn.accept(
            courseId,
            withSuccess(Fixtures.asString(
                INTEGRATION_TEST_OODI_FIXTURES_PREFIX,
                "course-realisation-itest.json",
                new ImmutableMap.Builder()
                    .put("courseId", courseId)
                    .put("students", students)
                    .put("teachers", teachers)
                    .put("endDate", getFutureDateString())
                    .build()),
                MediaType.APPLICATION_JSON));

        for (StudentUser studentUser : students) {
            expectFindStudentRequestToIAM(studentUser.studentNumber, studentUser.username);
        }

        for (TeacherUser teacherUser : teachers) {
            expectFindEmployeeRequestToIAM(TEACHER_ID_PREFIX + teacherUser.teacherId, teacherUser.username);
        }
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
        This role is never removed, even if other roles are removed. Note that only student role can be removed by Moodi
        if student is returned from Oodi with approved set to false.
     */

    protected void assertMoodiRoleEnrollment(String username, List<MoodleUserEnrollments> moodleUserEnrollmentsList) {
        assertUserEnrollments(
            username,
            moodleUserEnrollmentsList,
            singletonList(mapperService.getMoodiRoleId()));
    }
}
