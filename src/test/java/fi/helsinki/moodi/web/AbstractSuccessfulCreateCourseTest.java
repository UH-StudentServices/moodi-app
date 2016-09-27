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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public abstract class AbstractSuccessfulCreateCourseTest extends AbstractMoodiIntegrationTest {

    protected static final long COURSE_REALISATION_ID = 102374742L;
    protected final static long MOODLE_COURSE_ID = 988888L;
    private static final long MOODLE_USER_ID_1 = 1L;
    private static final long MOODLE_USER_ID_2 = 2L;
    private static final long MOODLE_USER_ID_3 = 3L;
    private static final long MOODLE_USER_ID_4 = 4L;
    private static final String MOODLE_USERNAME_1 = "niina@helsinki.fi";
    private static final String MOODLE_USERNAME_2 = "jukka@helsinki.fi";
    private static final String MOODLE_USERNAME_3 = "make@helsinki.fi";
    private static final String MOODLE_USERNAME_4 = "hraopettaja@helsinki.fi";


    protected void setUpMockServerResponses() {

        setupCommonResponses();

        expectCreateCourseRequestToMoodle(COURSE_REALISATION_ID, MOODLE_COURSE_ID);

        expectGetUserRequestToMoodle(MOODLE_USERNAME_1, MOODLE_USER_ID_1);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_2, MOODLE_USER_ID_2);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_3, MOODLE_USER_ID_3);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_4, MOODLE_USER_ID_4);

        expectEnrollmentsWithAddedMoodiRoles(Lists.newArrayList(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_1, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_2, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_3, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_ID_4, MOODLE_COURSE_ID)
            ));
    }

    private void expectEnrollmentsWithAddedMoodiRoles(List<MoodleEnrollment> moodleEnrollments) {
        List<MoodleEnrollment> moodleEnrollmentsWithMoodiRoles = moodleEnrollments.stream()
            .flatMap(enrollment -> Stream.of(enrollment, new MoodleEnrollment(getMoodiRoleId(), enrollment.moodleUserId, enrollment.moodleCourseId)))
            .collect(Collectors.toList());

        expectEnrollmentRequestToMoodle(moodleEnrollmentsWithMoodiRoles.toArray(new MoodleEnrollment[moodleEnrollmentsWithMoodiRoles.size()]));
    }

    protected void setUpMockServerResponsesWithWarnings() {
        setupCommonResponses();

        expectCreateCourseRequestToMoodle(COURSE_REALISATION_ID, MOODLE_COURSE_ID);

        expectGetUserRequestToMoodle(MOODLE_USERNAME_1, MOODLE_USER_ID_1);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_2, MOODLE_USER_ID_2);
        expectGetUserRequestToMoodleUserNotFound(MOODLE_USERNAME_3);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_4, MOODLE_USER_ID_4);

        expectEnrollmentsWithAddedMoodiRoles(Lists.newArrayList(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_1, MOODLE_COURSE_ID),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_2, MOODLE_COURSE_ID),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_ID_4, MOODLE_COURSE_ID)
            ));
    }

    private void setupCommonResponses() {
        expectGetCourseRealisationUnitRequestToOodi(
            COURSE_REALISATION_ID,
            withSuccess(Fixtures.asString("/oodi/course-realisation.json"), MediaType.APPLICATION_JSON));

        expectFindStudentRequestToEsb("010342729", "niina");
        expectFindStudentRequestToEsb("011119854", "jukka");
        expectFindStudentRequestToEsb("011524656", "make");
        expectFindEmployeeRequestToEsb("9110588", "hraopettaja");
    }
}
