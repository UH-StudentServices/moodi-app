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

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;

import static java.lang.Math.toIntExact;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CreateCourseTest extends AbstractSuccessfulCreateCourseTest {

    private static String COURSE_NOT_FOUND_MESSAGE = "Oodi course not found with realisation id %s";

    @Ignore
    @Test
    public void successfulCreateCourseReturnsCorrectResponse() throws Exception {
        setUpMockServerResponses();

        makeCreateCourseRequest(COURSE_REALISATION_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID)));
    }

    @Ignore
    @Test
    public void successfulCreateCourseInvokesCorrectIntegrationServices() throws Exception {
        setUpMockServerResponses();

        makeCreateCourseRequest(COURSE_REALISATION_ID).andReturn();
    }

    @Test
    public void thatImportFailsIfOodiReturnsNullForData() throws Exception {
        testEmptyOodiResponse(NULL_DATA_RESPONSE);
    }

    @Test
    public void thatImportFailsIfOodiReturnsEmptyString() throws Exception {
        testEmptyOodiResponse(EMPTY_OK_RESPONSE);
    }

    private void testEmptyOodiResponse(String response) throws Exception{
        expectGetCourseUnitRealisationRequestToOodi(
            COURSE_REALISATION_ID,
            withSuccess(response, MediaType.APPLICATION_JSON));

        makeCreateCourseRequest(COURSE_REALISATION_ID)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                .value(String.format(COURSE_NOT_FOUND_MESSAGE, COURSE_REALISATION_ID)));
    }
}


