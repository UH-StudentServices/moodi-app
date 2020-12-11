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
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public abstract class AbstractSuccessfulCreateCourseTest extends AbstractMoodiIntegrationTest {

    protected static final String OODI_COURSE_REALISATION_ID = "102374742";
    protected static final String SISU_COURSE_REALISATION_ID = "hy-CUR-123";

    protected static final String STUDENT_NUMBER_NIINA = "010342729";
    protected static final String STUDENT_NUMBER_JUKKA = "011119854";
    protected static final String STUDENT_NUMBER_MAKE = "011524656";
    protected static final String EMP_NUMBER_HRAOPE = "9110588";

    protected static final String ESB_USERNAME_NIINA = "niina";
    protected static final String ESB_USERNAME_JUKKA = "jukka";
    protected static final String ESB_USERNAME_MAKE = "make";
    protected static final String ESB_USERNAME_HRAOPE = "hraopettaja";

    protected static final String EXPECTED_OODI_DESCRIPTION_TO_MOODLE = "Description+1+%28fi%29+Description+2+%28fi%29";
    protected static final String EXPECTED_SISU_DESCRIPTION_TO_MOODLE = "https%3A%2F%2Fcourses.helsinki.fi%2Ffi%2FOODI-FLOW%2F136394381";

    protected void setUpMockServerResponsesForOodiCourse() {
        setUpOodiResponse();
        setupIAMResponses();
        setUpMoodleResponses(OODI_COURSE_REALISATION_ID, EXPECTED_OODI_DESCRIPTION_TO_MOODLE, "oodi_");
    }

    protected void expectEnrollmentsWithAddedMoodiRoles(List<MoodleEnrollment> moodleEnrollments) {
        List<MoodleEnrollment> moodleEnrollmentsWithMoodiRoles = moodleEnrollments.stream()
            .flatMap(enrollment -> Stream.of(enrollment, new MoodleEnrollment(getMoodiRoleId(), enrollment.moodleUserId, enrollment.moodleCourseId)))
            .collect(Collectors.toList());

        expectEnrollmentRequestToMoodle(moodleEnrollmentsWithMoodiRoles.toArray(new MoodleEnrollment[moodleEnrollmentsWithMoodiRoles.size()]));
    }

    protected void setUpMockServerResponsesWithWarningsForOodiCourse() {
        setUpOodiResponse();
        setupIAMResponses();

        expectCreateCourseRequestToMoodle(OODI_COURSE_REALISATION_ID, "oodi_", EXPECTED_OODI_DESCRIPTION_TO_MOODLE, MOODLE_COURSE_ID);

        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA);
        expectGetUserRequestToMoodleUserNotFound(MOODLE_USERNAME_MAKE);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentsWithAddedMoodiRoles(Lists.newArrayList(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID)
            ));
    }

    protected void setUpMockServerResponsesForSisuCourse() {
        setUpSisuResponsesFor123();
        setUpMoodleResponses(SISU_COURSE_REALISATION_ID, EXPECTED_SISU_DESCRIPTION_TO_MOODLE, "sisu_");
    }

    protected void setUpSisuResponsesFor123() {
        mockSisuServer.expectCourseUnitRealisationRequest(SISU_COURSE_REALISATION_ID, "/sisu/course-unit-realisation.json");
        mockSisuServer.expectPersonsRequest(Arrays.asList("hy-hlo-4"), "/sisu/persons.json");
    }

    private void setUpOodiResponse() {
        expectGetCourseUnitRealisationRequestToOodi(
            OODI_COURSE_REALISATION_ID,
            withSuccess(Fixtures.asString("/oodi/course-realisation.json"), MediaType.APPLICATION_JSON));
    }

    private void setUpMoodleResponses(String curId, String description, String moodleCourseIdPrefix) {
        expectCreateCourseRequestToMoodle(curId, moodleCourseIdPrefix, description, MOODLE_COURSE_ID);

        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_MAKE, MOODLE_USER_ID_MAKE);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentsWithAddedMoodiRoles(Lists.newArrayList(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_MAKE, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID)
        ));
    }

    private void setupIAMResponses() {
        expectFindStudentRequestToIAM(STUDENT_NUMBER_NIINA, ESB_USERNAME_NIINA);
        expectFindStudentRequestToIAM(STUDENT_NUMBER_JUKKA, ESB_USERNAME_JUKKA);
        expectFindStudentRequestToIAM(STUDENT_NUMBER_MAKE, ESB_USERNAME_MAKE);
        expectFindEmployeeRequestToIAM(EMP_NUMBER_HRAOPE, ESB_USERNAME_HRAOPE);
    }
}
