package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleClientGetEnrolledUsersTest extends AbstractMoodiIntegrationTest {

    private static final String STUDENT_MOODLE_ROLE_NAME = "Student";
    private static final String MOODI_ROLE_NAME = "Moodi synced";

    @Autowired
    private MoodleClient moodleClient;

    @Test
    public void deserializeRespose() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users&moodlewsrestformat=json&courseid=1234"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/get-enrolled-users.json"), MediaType.APPLICATION_JSON));

        final List<MoodleUserEnrollments> users = moodleClient.getEnrolledUsers(1234);
        assertEquals(8, users.size());

        final MoodleUserEnrollments user = users.get(0);
        assertEquals(Long.valueOf(5), user.id);

        assertEquals(2, user.roles.size());

        final MoodleRole moodleStudentRole = user.roles.get(0);
        assertEquals(getStudentRoleId(), moodleStudentRole.roleId);
        assertEquals(STUDENT_MOODLE_ROLE_NAME, moodleStudentRole.name);

        final MoodleRole moodiRole = user.roles.get(1);
        assertEquals(getMoodiRoleId(), moodiRole.roleId);
        assertEquals(MOODI_ROLE_NAME, moodiRole.name);
    }
}