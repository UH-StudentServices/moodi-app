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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractSuccessfulCreateCourseTest extends AbstractMoodiIntegrationTest {
    protected static final String EXPECTED_SISU_DESCRIPTION_TO_MOODLE = "https%3A%2F%2Fcourses.helsinki.fi%2Ffi%2FOODI-FLOW%2F136394381";

    protected void expectEnrollmentsWithAddedMoodiRoles(List<MoodleEnrollment> moodleEnrollments) {
        expectEnrollmentRequestToMoodle(moodleEnrollments.stream()
            .flatMap(enrollment -> Stream.of(enrollment,
                new MoodleEnrollment(getMoodiRoleId(), enrollment.moodleUserId, enrollment.moodleCourseId))).toArray(MoodleEnrollment[]::new));
    }

    protected void setUpMockServerResponsesForSisuCourse123(boolean allUsersFound, String creatorSisuId) {
        setUpSisuResponsesFor123(creatorSisuId);
        expectSisuOrganisationExportRequest();
        setUpMoodleResponses(SISU_REALISATION_NOT_IN_DB_ID, EXPECTED_SISU_DESCRIPTION_TO_MOODLE, allUsersFound, "9", creatorSisuId);
    }

    protected void setUpSisuResponseCourse123NotFound() {
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(Arrays.asList(SISU_REALISATION_NOT_IN_DB_ID),
                "/sisu/course-unit-realisation-not-found.json");
    }

    protected void setUpSisuResponsesFor123(String creatorSisuId) {
        if (creatorSisuId != null) {
            setUpGetCreatorCall(creatorSisuId);
        }
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(Arrays.asList(SISU_REALISATION_NOT_IN_DB_ID),
                "/sisu/course-unit-realisation-not-in-db.json");
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList("hy-hlo-4"), "/sisu/persons.json");
    }

    protected void setUpGetCreatorCall(String creatorSisuId) {
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList(creatorSisuId),
            CREATOR_SISU_ID.equals(creatorSisuId) ?
                "/sisu/persons-hy-hlo-creator.json" :
                "/sisu/persons-not-found.json");
    }

    protected void setUpMoodleResponses(String curId, String description,
                                        boolean allUsersFound, String categoryId, String creatorSisuId) {
        expectCreateCourseRequestToMoodle(curId, description, MOODLE_COURSE_ID_NOT_IN_DB, categoryId);

        expectGetUserRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectGetUserRequestToMoodle(MOODLE_USERNAME_JUKKA, MOODLE_USER_ID_JUKKA);
        if (allUsersFound) {
            expectGetUserRequestToMoodle(MOODLE_USERNAME_MAKE, MOODLE_USER_ID_MAKE);
        } else {
            expectGetUserRequestToMoodleUserNotFound(MOODLE_USERNAME_MAKE);
        }
        expectGetUserRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);
        if (creatorSisuId != null) {
            expectGetUserRequestToMoodle(MOODLE_USERNAME_CREATOR, MOODLE_USER_CREATOR);
        }

        List<MoodleEnrollment> expectedEnrollments = Lists.newArrayList(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA, MOODLE_COURSE_ID_NOT_IN_DB),
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_JUKKA, MOODLE_COURSE_ID_NOT_IN_DB),
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_NOT_IN_DB)
        );

        if (allUsersFound) {
            expectedEnrollments.add(2, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_MAKE, MOODLE_COURSE_ID_NOT_IN_DB));
        }
        if (creatorSisuId != null) {
            expectedEnrollments.add(new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_CREATOR, MOODLE_COURSE_ID_NOT_IN_DB));
        }
        expectEnrollmentsWithAddedMoodiRoles(expectedEnrollments);
    }
}
