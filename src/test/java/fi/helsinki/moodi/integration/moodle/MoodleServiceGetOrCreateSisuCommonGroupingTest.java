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
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleServiceGetOrCreateSisuCommonGroupingTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleService moodleService;

    @Test
    public void testNonExisting() {
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_group_get_course_groupings&moodlewsrestformat=json&courseid=123"))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(
                "wstoken=xxxx1234&wsfunction=core_group_create_groupings&moodlewsrestformat=json"
                + "&groupings%5B0%5D%5Bname%5D=Sisusta+synkronoidut&groupings%5B0%5D%5Bcourseid%5D=123&groupings%5B0%5D%5Bdescription%5D"
                + "&groupings%5B0%5D%5Bdescriptionformat%5D=1&groupings%5B0%5D%5Bidnumber%5D=sisu-synchronised"
            ))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/create-sisu-common-grouping.json"), MediaType.APPLICATION_JSON));

        final MoodleGrouping grouping = moodleService.getOrCreateSisuCommonGrouping(123L, "fi");
        Assert.assertTrue(grouping.getId() > 0);
    }
}
