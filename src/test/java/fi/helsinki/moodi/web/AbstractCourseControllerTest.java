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

import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.MoodleCourseBuilder;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class AbstractCourseControllerTest extends AbstractMoodiIntegrationTest {

    private static final String EMPTY_LIST_RESPONSE = "[]";
    private static final String EXPECTED_COURSE_ID = MoodleCourseBuilder.MOODLE_COURSE_ID_PREFIX + 102374742;

    protected final ResultActions makeCreateCourseRequest(final long realisationId) throws Exception {
        return mockMvc.perform(
                post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("client-id", "testclient")
                        .header("client-token", "xxx123")
                        .content(toJson(new ImportCourseRequest(realisationId))));
    }

    protected final void expectFindStudentRequestToEsb(final String studentNumber, final String username) {
        final String response = "[{\"username\":\"" + username + "\",\"studentNumber\":\"" + studentNumber + "\"}]";
        esbMockServer.expect(requestTo("https://esbmt1.it.helsinki.fi/iam/findStudent/" + studentNumber))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected final void expectFindEmployeeRequestToEsb(final String teacherId, final String username) {
        final String response = "[{\"username\":\"" + username + "\",\"personnelNumber\":\"" + teacherId + "\"}]";
        esbMockServer.expect(requestTo("https://esbmt1.it.helsinki.fi/iam/findEmployee/" + teacherId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected final void expectGetUserRequestToMoodleUserNotFound(final String username) {
        expectGetUserRequestToMoodleWithResponse(username, EMPTY_LIST_RESPONSE);
    }

    protected final void expectGetUserRequestToMoodle(final String username, final long userMoodleId) {
        expectGetUserRequestToMoodle(username, String.valueOf(userMoodleId));
    }

    protected final void expectGetUserRequestToMoodle(final String username, final String userMoodleId) {
        final String response = String.format("[{\"id\":\"%s\", \"username\":\"%s\", \"email\":\"\", \"fullname\":\"\"}]", userMoodleId, username);
        expectGetUserRequestToMoodleWithResponse(username, response);
    }

    private void expectGetUserRequestToMoodleWithResponse(String username, String response) {
        String payload = "wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json&field=username&values%5B%5D=" + urlEncode(username);

        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(payload))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected final void expectGetCourseRealisationUnitRequestToOodi(final long realisationId, final ResponseCreator responseCreator) {
        final String url = "https://oprek4.it.helsinki.fi:30039/courseunitrealisations/" + realisationId;
        oodiMockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(responseCreator);
    }

    protected final void expectCreateCourseRequestToMoodle(final long realisationId, final long moodleCourseIdToReturn) {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andExpect(content().string(
                    "wstoken=xxxx1234&wsfunction=core_course_create_courses&moodlewsrestformat=json&courses%5B0%5D%5Bidnumber%5D=" +
                    EXPECTED_COURSE_ID +
                    "&courses%5B0%5D%5Bfullname%5D=Lapsuus+ja+yhteiskunta&courses%5B0%5D%5Bshortname%5D=Lapsuus++" +
                    realisationId +
                    "&courses%5B0%5D%5Bcategoryid%5D=4&courses%5B0%5D%5Bsummary%5D=Description+1+%28fi%29+Description+2+%28fi%29&courses%5B0%5D%5Bformat%5D=topics&courses%5B0%5D%5Bmaxbytes%5D=20971520&courses%5B0%5D%5Bshowgrades%5D=1&courses%5B0%5D%5Bvisible%5D=0&courses%5B0%5D%5Bnewsitems%5D=5&courses%5B0%5D%5Bnumsections%5D=7&courses%5B0%5D%5Bshowreports%5D=0"))
                .andRespond(withSuccess("[{\"id\":\"" + moodleCourseIdToReturn + "\", \"shortname\":\"shortie\"}]", MediaType.APPLICATION_JSON));
    }

    protected final void expectGetEnrollmentsRequestToMoodle(final int courseId) {

        final String response = "[]";
        final String payload = "wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users&moodlewsrestformat=json&courseid=" + courseId;

        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andExpect(content().string(payload))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private String urlEncode(final String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}