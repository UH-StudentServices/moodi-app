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

    private static String COURSE_NOT_FOUND_MESSAGE = "Study registry course not found with realisation id %s";

    @Test
    public void successfulCreateCourseReturnsCorrectResponse() throws Exception {
        setUpMockServerResponsesForOodiCourse();

        makeCreateCourseRequest(OODI_COURSE_REALISATION_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moodleCourseId").value(toIntExact(MOODLE_COURSE_ID)));
    }

    @Test
    public void successfulCreateCourseBySisuIDReturnsCorrectResponse() throws Exception {
        setUpMockServerResponsesForSisuCourse();

        makeCreateCourseRequest(SISU_COURSE_REALISATION_ID)
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
            OODI_COURSE_REALISATION_ID,
            withSuccess(Fixtures.asString("/oodi/course-realisation-automatic-enabled.json"), MediaType.APPLICATION_JSON));

        expectCreateCourseRequestToMoodle(OODI_COURSE_REALISATION_ID, "oodi_", EXPECTED_OODI_DESCRIPTION_TO_MOODLE, MOODLE_COURSE_ID);

        expectFindStudentRequestToIAM(STUDENT_NUMBER_NIINA, ESB_USERNAME_NIINA);
        expectFindStudentRequestToIAM(STUDENT_NUMBER_JUKKA, ESB_USERNAME_JUKKA);
        expectFindEmployeeRequestToIAM(EMP_NUMBER_HRAOPE, ESB_USERNAME_HRAOPE);

        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentsWithAddedMoodiRoles(Lists.newArrayList(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID)
            ));

        makeCreateCourseRequest(OODI_COURSE_REALISATION_ID)
            .andExpect(status().isOk());
    }

    private void testEmptyOodiResponse(String response) throws Exception {
        expectGetCourseUnitRealisationRequestToOodi(
            OODI_COURSE_REALISATION_ID,
            withSuccess(response, MediaType.APPLICATION_JSON));

        makeCreateCourseRequest(OODI_COURSE_REALISATION_ID)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                .value(String.format(COURSE_NOT_FOUND_MESSAGE, OODI_COURSE_REALISATION_ID)));
    }
}


