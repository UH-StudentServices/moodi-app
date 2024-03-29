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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleClientGetEnrolledUsersTest extends AbstractMoodiIntegrationTest {

    private static final String STUDENT_MOODLE_ROLE_NAME = "Student";
    private static final String MOODI_ROLE_NAME = "Moodi synced";

    @Autowired
    private MoodleClient moodleClient;

    @Test
    public void deserializeResponse() {
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users&moodlewsrestformat=json&courseid=1234"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/get-enrolled-users.json"), MediaType.APPLICATION_JSON));

        final List<MoodleUserEnrollments> users = moodleClient.getEnrolledUsers(1234);
        assertEquals(8, users.size());

        final MoodleUserEnrollments user = users.get(0);
        assertEquals(Long.valueOf(5), user.id);

        assertEquals(2, user.roles.size());

        assertEquals(2, user.enrolledCourses.size());
        assertTrue(user.seesCourse(1234L));
        assertTrue(user.seesCourse(999L));
        assertFalse(user.seesCourse(888L));

        final MoodleRole moodleStudentRole = user.roles.get(0);
        assertEquals(getStudentRoleId(), moodleStudentRole.roleId);
        assertEquals(STUDENT_MOODLE_ROLE_NAME, moodleStudentRole.name);

        final MoodleRole moodiRole = user.roles.get(1);
        assertEquals(getMoodiRoleId(), moodiRole.roleId);
        assertEquals(MOODI_ROLE_NAME, moodiRole.name);
    }
}
