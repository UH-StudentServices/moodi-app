package fi.helsinki.moodi.integration.moodle;

import com.google.common.collect.Lists;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleClientEnrollToCourseTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleClient moodleClient;


    @Test
    public void enrollWithInsufficientPermissions() {



        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=enrol_manual_enrol_users&moodlewsrestformat=json&enrolments%5B0%5D%5Bcourseid%5D=12345&enrolments%5B0%5D%5Broleid%5D=9&enrolments%5B0%5D%5Buserid%5D=3"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/enrol-user-no-permissions.json"), MediaType.APPLICATION_JSON));

        try {
            moodleClient.enrollToCourse(Lists.newArrayList(new MoodleEnrollment(9, 3, 12345)));
            fail("We want an exception!");
        } catch (MoodleClientException e) {
            assertEquals("You don't have the permission to assign this role (9) to this user (5) in this course(12).", e.getMessage());
            assertEquals("moodle_exception", e.getMoodleException());
            assertEquals("wsusercannotassign", e.getErrorCode());
        }
    }
}