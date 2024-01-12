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
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleServiceGetGroupingsWithGroupsTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleService moodleService;

    @Test
    public void testGetGroupingsWithGroupsAndMembers() {
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_group_get_course_groupings&moodlewsrestformat=json&courseid=123"))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-course-groupings.json"), MediaType.APPLICATION_JSON));
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(
                "wstoken=xxxx1234&wsfunction=core_group_get_groupings&moodlewsrestformat=json"
                + "&groupingids%5B0%5D=15&groupingids%5B1%5D=16&groupingids%5B2%5D=14&returngroups=1"
            ))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-groupings-with-groups.json"), MediaType.APPLICATION_JSON));
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(
                "wstoken=xxxx1234&wsfunction=core_group_get_group_members&moodlewsrestformat=json"
                    + "&groupids%5B0%5D=196&groupids%5B1%5D=197&groupids%5B2%5D=198&groupids%5B3%5D=199&groupids%5B4%5D=196"
                    + "&groupids%5B5%5D=197&groupids%5B6%5D=198&groupids%5B7%5D=199"
            ))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-group-members.json"), MediaType.APPLICATION_JSON));
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(
                "wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json"
                    + "&field=id&values%5B0%5D=441&values%5B1%5D=312&values%5B2%5D=310&values%5B3%5D=315&values%5B4%5D=468&values%5B5%5D=467"
                    + "&values%5B6%5D=311&values%5B7%5D=284&values%5B8%5D=320&values%5B9%5D=419&values%5B10%5D=303&values%5B11%5D=299"
                    + "&values%5B12%5D=294&values%5B13%5D=306&values%5B14%5D=434&values%5B15%5D=446&values%5B16%5D=463&values%5B17%5D=466"
                    + "&values%5B18%5D=437&values%5B19%5D=436&values%5B20%5D=447&values%5B21%5D=455&values%5B22%5D=301&values%5B23%5D=435"
                    + "&values%5B24%5D=302&values%5B25%5D=433&values%5B26%5D=295&values%5B27%5D=297&values%5B28%5D=305&values%5B29%5D=298"
                    + "&values%5B30%5D=431&values%5B31%5D=471&values%5B32%5D=300&values%5B33%5D=448&values%5B34%5D=460&values%5B35%5D=359"
            ))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-users-by-field.json"), MediaType.APPLICATION_JSON));

        final List<MoodleGrouping> groupings = moodleService.getGroupingsWithGroups(123L, true);
        assertEquals(3, groupings.size());
        assertEquals(2, groupings.get(0).getGroups().size());
        assertEquals(2, groupings.get(1).getGroups().size());
        assertEquals(4, groupings.get(2).getGroups().size());
        assertEquals(0, groupings.get(0).getGroups().get(0).getMembers().size());
        assertEquals(10, groupings.get(0).getGroups().get(1).getMembers().size());
        assertEquals(15, groupings.get(1).getGroups().get(0).getMembers().size());
        assertEquals(11, groupings.get(1).getGroups().get(1).getMembers().size());
        assertTrue(groupings.get(1).getGroups().get(0).getMembers().get(0).getUsername().length() > 1);
    }
}
