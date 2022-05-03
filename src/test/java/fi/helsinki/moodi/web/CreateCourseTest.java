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
    private static String PERSON_NOT_FOUND_MESSAGE = "Sisu person not found with id %s";

    @Test
    public void successfulCreateCourseBySisuIDReturnsCorrectResponse() throws Exception {
        setUpMockServerResponsesForSisuCourse123(true, null);

        makeCreateCourseRequest(SISU_REALISATION_NOT_IN_DB_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID_NOT_IN_DB)));
    }

    @Test
    public void thatImportFailsIfCourseNotFoundInSisu() throws Exception {
        setUpSisuResponseCourse123NotFound();

        makeCreateCourseRequest(SISU_REALISATION_NOT_IN_DB_ID)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                .value(String.format(COURSE_NOT_FOUND_MESSAGE, SISU_REALISATION_NOT_IN_DB_ID, SISU_REALISATION_NOT_IN_DB_ID)));
    }

    @Test
    public void thatIfFirstImportFailsTheSecondImportCanSucceed() throws Exception {
        // Set up first try to get a server error from Moodle.
        setUpSisuResponsesFor123(null);
        expectSisuOrganisationExportRequest();

        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(startsWith(
                "wstoken=xxxx1234&wsfunction=core_course_create_courses")))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andRespond(withServerError());

        // Set up second try to succeed. Sisu organisation call is not made, as it is cached.
        setUpSisuResponsesFor123(null);
        setUpMoodleResponses(SISU_REALISATION_NOT_IN_DB_ID, EXPECTED_SISU_DESCRIPTION_TO_MOODLE, true, "9", null);

        // First try.
        makeCreateCourseRequest(SISU_REALISATION_NOT_IN_DB_ID)
            // Why return client error when Moodle is broken? I don't know, but not touching that right now.
            .andExpect(status().is4xxClientError());

        try {
            importingService.getImportedCourse(SISU_REALISATION_NOT_IN_DB_ID);
            fail("Getting a course not in Moodle should have failed.");
        } catch (CourseNotFoundException e) {
            // Good.
        }

        // Second try.
        makeCreateCourseRequest(SISU_REALISATION_NOT_IN_DB_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID_NOT_IN_DB)));

        assertEquals("COMPLETED", importingService.getImportedCourse(SISU_REALISATION_NOT_IN_DB_ID).importStatus);
    }

    @Test
    public void thatImportingWithCreatorSisuIdWorks() throws Exception {
        setUpMockServerResponsesForSisuCourse123(true, CREATOR_SISU_ID);

        makeCreateCourseRequest(SISU_REALISATION_NOT_IN_DB_ID, CREATOR_SISU_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID_NOT_IN_DB)));
    }

    @Test
    public void thatImportingWithNotFoundCreatorSisuIdThrowsException() throws Exception {
        setUpGetCreatorCall(PERSON_NOT_FOUND_ID);

        makeCreateCourseRequest(SISU_REALISATION_NOT_IN_DB_ID, PERSON_NOT_FOUND_ID)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                .value(String.format(PERSON_NOT_FOUND_MESSAGE, PERSON_NOT_FOUND_ID)));
    }
}


