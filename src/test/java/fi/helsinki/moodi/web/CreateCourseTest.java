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

import fi.helsinki.moodi.exception.CourseNotFoundException;
import fi.helsinki.moodi.service.importing.ImportingService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import static java.lang.Math.toIntExact;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CreateCourseTest extends AbstractSuccessfulCreateCourseTest {

    @Autowired
    private ImportingService importingService;

    private static String COURSE_NOT_FOUND_MESSAGE = "Study registry course not found with realisation id %s (%s)";

    @Test
    public void successfulCreateCourseBySisuIDReturnsCorrectResponse() throws Exception {
        setUpMockServerResponsesForSisuCourse123(true);

        makeCreateCourseRequest(SISU_COURSE_REALISATION_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID)));
    }

    @Test
    public void successfulCreateCourseByOodiNativeIDUsesSisuAndReturnsCorrectResponse() throws Exception {
        setUpMockServerResponsesForSisuCourse123(true);

        makeCreateCourseRequest("123")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID)));
    }

    @Test
    public void thatImportFailsIfCourseNotFoundInSisu() throws Exception {
        setUpSisuResponseCourse123NotFound();

        makeCreateCourseRequest(SISU_COURSE_REALISATION_ID)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                .value(String.format(COURSE_NOT_FOUND_MESSAGE, SISU_COURSE_REALISATION_ID, SISU_COURSE_REALISATION_ID)));
    }

    @Test
    public void thatImportFailsIfCourseNotFoundInSisuWithOptimeNativeID() throws Exception {
        setUpSisuResponseCourse123NotFound();
        String oodiCourseFromOptime = "123";

        makeCreateCourseRequest(oodiCourseFromOptime)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                .value(String.format(COURSE_NOT_FOUND_MESSAGE, SISU_COURSE_REALISATION_ID, oodiCourseFromOptime)));
    }

    @Test
    public void thatIfFirstImportFailsTheSecondImportCanSucceed() throws Exception {
        // Set up first try to get a server error from Moodle.
        setUpSisuResponsesFor123();
        expectSisuOrganisationExportRequest();

        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(startsWith(
                "wstoken=xxxx1234&wsfunction=core_course_create_courses")))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andRespond(withServerError());

        // Set up second try to succeed. Sisu organisation call is not made, as it is cached.
        setUpSisuResponsesFor123();
        setUpMoodleResponses(SISU_COURSE_REALISATION_ID, EXPECTED_SISU_DESCRIPTION_TO_MOODLE, "sisu_", true, "9");

        // First try.
        makeCreateCourseRequest(SISU_COURSE_REALISATION_ID)
            // Why return client error when Moodle is broken? I don't know, but not touching that right now.
            .andExpect(status().is4xxClientError());

        try {
            importingService.getImportedCourse(SISU_COURSE_REALISATION_ID);
            fail("Getting a course not in Moodle should have failed.");
        } catch (CourseNotFoundException e) {
            // Good.
        }

        // Second try.
        makeCreateCourseRequest(SISU_COURSE_REALISATION_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID)));

        assertEquals("COMPLETED", importingService.getImportedCourse(SISU_COURSE_REALISATION_ID).importStatus);
    }
}


