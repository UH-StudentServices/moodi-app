package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleClientGetUsersTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleClient moodleClient;

    @Test
    public void deserializeRespose() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json&field=username&values%5B%5D=integraatio"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/get-users.json"), MediaType.APPLICATION_JSON));

        final MoodleUser user = moodleClient.getUser("integraatio");
        assertNotNull(user);

        assertEquals("marja.kari@iki.fi", user.email);
        assertEquals("Testi Integraatio", user.fullname);
        assertEquals("integraatio", user.username);
        assertEquals(Long.valueOf(3), user.id);
    }
}