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

import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.http.MediaType;

import static java.lang.Math.toIntExact;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CreateCourseTest extends AbstractSuccessfulCreateCourseTest {

    private static String COURSE_NOT_FOUND_MESSAGE = "Oodi course not found with realisation id %s";

    @Test
    public void successfulCreateCourseReturnsCorrectResponse() throws Exception {
        setUpMockServerResponses();

        makeCreateCourseRequest(COURSE_REALISATION_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID)));
    }

    @Test
    public void thatImportFailsIfOodiReturnsNullForData() throws Exception {
        testEmptyOodiResponse(OODI_NULL_DATA_RESPONSE);
    }

    @Test
    public void thatImportFailsIfOodiReturnsEmptyString() throws Exception {
        testEmptyOodiResponse(EMPTY_RESPONSE);
    }

    @Test
    public void thatAutomaticEnabledCourseEnrollmentsAreMadeCorrectly() throws Exception {
        expectGetCourseUnitRealisationRequestToOodi(
            COURSE_REALISATION_ID,
            withSuccess(Fixtures.asString("/oodi/course-realisation-automatic-enabled.json"), MediaType.APPLICATION_JSON));

        expectCreateCourseRequestToMoodle(COURSE_REALISATION_ID, MOODLE_COURSE_ID);

        expectFindStudentRequestToIAM(STUDENT_NUMBER_1, ESB_USERNAME_1);
        expectFindStudentRequestToIAM(STUDENT_NUMBER_2, ESB_USERNAME_2);
        expectFindEmployeeRequestToIAM(TEACHER_ID, TEACHER_ESB_USERNAME);

        expectGetUserRequestToMoodle(MOODLE_USERNAME_1, MOODLE_USER_ID_1);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_2, MOODLE_USER_ID_2);
        expectGetUserRequestToMoodle(TEACHER_MOODLE_USERNAME, TEACHER_MOODLE_USER_ID);

        expectEnrollmentsWithAddedMoodiRoles(Lists.newArrayList(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_1, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_2, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_MOODLE_USER_ID, MOODLE_COURSE_ID)
            ));

        makeCreateCourseRequest(COURSE_REALISATION_ID)
            .andExpect(status().isOk());
    }

    private void testEmptyOodiResponse(String response) throws Exception {
        expectGetCourseUnitRealisationRequestToOodi(
            COURSE_REALISATION_ID,
            withSuccess(response, MediaType.APPLICATION_JSON));

        makeCreateCourseRequest(COURSE_REALISATION_ID)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                .value(String.format(COURSE_NOT_FOUND_MESSAGE, COURSE_REALISATION_ID)));
    }
}


