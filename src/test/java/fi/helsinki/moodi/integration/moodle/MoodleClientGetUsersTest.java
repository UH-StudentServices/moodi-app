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

package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleClientGetUsersTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleClient moodleClient;

    @Test
    public void deserializeRespose() {
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json" +
                    "&field=username&values%5B0%5D=integraatio"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/get-users-one-username.json"), MediaType.APPLICATION_JSON));

        MoodleUser user = moodleClient.getUser(Arrays.asList("integraatio"));
        assertNotNull(user);

        assertEquals(Long.valueOf(3), user.getId());
    }

    @Test
    public void getWithTheSecondUsername() {
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json" +
                    "&field=username&values%5B0%5D=first&values%5B1%5D=second"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/get-users-second-username.json"), MediaType.APPLICATION_JSON));

        MoodleUser user = moodleClient.getUser(Arrays.asList("first", "second"));
        assertNotNull(user);

        assertEquals(Long.valueOf(2), user.getId());
    }
}
