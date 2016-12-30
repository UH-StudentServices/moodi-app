package fi.helsinki.moodi.moodle;

import fi.helsinki.moodi.integration.moodle.MoodleClient;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportingService;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;
import java.util.Random;

import static fi.helsinki.moodi.integration.esb.EsbService.DOMAIN_SUFFIX;
import static java.lang.Math.abs;

public abstract class AbstractMoodleIntegrationTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private ImportingService importingService;

    @Autowired
    protected MoodleClient moodleClient;

    @Autowired
    protected MapperService mapperService;

    protected static final String STUDENT_NUMBER = "014010293";
    protected static final String STUDENT_USERNAME = "bar_simp";
    protected static final String TEACHER_ID = "011631484";
    protected static final String TEACHER_USERNAME = "mag_simp";
    protected static final String STUDENT_NUMBER_2 = "011911609";
    protected static final String STUDENT_USERNAME_2 = "mag_simp";

    protected static final String INTEGRATION_TEST_OODI_FIXTURES_PREFIX = "src/itest/resources/fixtures/oodi/";


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

}