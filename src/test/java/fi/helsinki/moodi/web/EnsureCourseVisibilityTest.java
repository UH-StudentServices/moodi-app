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

package fi.helsinki.moodi.web;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EnsureCourseVisibilityTest extends AbstractMoodiIntegrationTest {
    private void mockUpdateCourseVisibility(long moodleId) {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andExpect(content().string(startsWith(
                "wstoken=xxxx1234&wsfunction=core_course_update_courses&moodlewsrestformat=json")))
            .andRespond(request -> withSuccess("[{\"id\":\"" + moodleId + "\", \"shortname\":\"shortie\"}]", MediaType.APPLICATION_JSON)
            .createResponse(request));
    }

    @Test
    public void successfulMakeVisibleReturnsCorrectResponse() throws Exception {
        mockUpdateCourseVisibility(MOODLE_COURSE_ID_IN_DB);
        MvcResult result = mockMvc.perform(
            post("/api/v1/courses/make_visible/" + SISU_REALISATION_IN_DB_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123")
                ).andExpect(status().isOk())
            .andReturn();
        String content = result.getResponse().getContentAsString();
        assertEquals("true", content);
    }

    @Test
    public void makeVisibleForNonexistingCourseReturnsNotFound() throws Exception {
        mockMvc.perform(
                post("/api/v1/courses/make_visible/" + SISU_REALISATION_NOT_IN_DB_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("client-id", "testclient")
                    .header("client-token", "xxx123")
            ).andExpect(status().isNotFound());
    }
}
