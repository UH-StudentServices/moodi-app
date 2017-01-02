package fi.helsinki.moodi.moodle;

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportingService;
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

import static fi.helsinki.moodi.integration.esb.EsbService.DOMAIN_SUFFIX;
import static fi.helsinki.moodi.integration.esb.EsbService.TEACHER_ID_PREFIX;
import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static java.lang.Math.abs;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public abstract class AbstractMoodleIntegrationTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private ImportingService importingService;

    @Autowired
    protected MoodleClient moodleClient;

    @Autowired
    protected MapperService mapperService;

    protected static final String STUDENT_USERNAME = "bar_simp";
    protected static final String TEACHER_USERNAME = "mag_simp";

    protected static final String INTEGRATION_TEST_OODI_FIXTURES_PREFIX = "src/itest/resources/fixtures/oodi/";

    protected final StudentUser studentUser = new StudentUser(STUDENT_USERNAME, "014010293", true);
    protected final StudentUser teacherInStudentRole = new StudentUser(TEACHER_USERNAME, "011911609", true);
    protected final TeacherUser teacherUser = new TeacherUser(TEACHER_USERNAME, "011631484");


    @Before
    @Override
    public final void setUpMockServers() {
        oodiMockServer = MockRestServiceServer.createServer(oodiRestTemplate);
        esbMockServer = MockRestServiceServer.createServer(esbRestTemplate);
    }

    @After
    @Override
    public final void verify() {
        oodiMockServer.verify();
        esbMockServer.verify();
    }

    protected long getOodiCourseId() {
        return abs(new Random().nextInt());
    }

    protected long importCourse(long courseId) {
        return importingService.importCourse(new ImportCourseRequest(courseId)).data
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

    protected void expectCourseRealisationWithUsers(long courseId, List<StudentUser> students, List<TeacherUser> teachers) {
        expectCourseRealisationWithUsers(this::expectGetCourseUnitRealisationRequestToOodi, courseId, students, teachers);
    }

    protected void expectCourseUsersWithUsers(long courseId, List<StudentUser> students, List<TeacherUser> teachers) {
        expectCourseRealisationWithUsers(this::expectGetCourseUsersRequestToOodi, courseId, students, teachers);
    }

    private void expectCourseRealisationWithUsers(
        BiConsumer<Long, ResponseCreator> expectationFn,
        long courseId,
        List<StudentUser> students,
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

        for(StudentUser studentUser : students) {
            expectFindStudentRequestToEsb(studentUser.studentNumber, studentUser.username);
        }

        for(TeacherUser teacherUser : teachers) {
            expectFindEmployeeRequestToEsb(TEACHER_ID_PREFIX + teacherUser.teacherId, teacherUser.username);
        }

    }



}