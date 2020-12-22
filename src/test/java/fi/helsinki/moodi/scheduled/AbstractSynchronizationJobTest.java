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

package fi.helsinki.moodi.scheduled;

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import fi.helsinki.moodi.service.synchronize.notify.LockedSynchronizationItemMessageBuilder;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem.UserSynchronizationItemStatus;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSender;

import java.util.List;

import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public abstract class AbstractSynchronizationJobTest extends AbstractMoodiIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMoodiIntegrationTest.class);

    protected static final String REALISATION_ID = "12345";
    protected static final int MOODLE_COURSE_ID = 54321;
    protected static final int SOME_OTHER_MOODLE_COURSE_ID = 999;
    protected static final int STUDENT_USER_MOODLE_ID = 555;
    protected static final int TEACHER_USER_MOODLE_ID = 3434;
    protected static final String STUDENT_NUMBER = "010342729";
    protected static final String EMPLOYEE_NUMBER = "110588";
    protected static final String EMPLOYEE_NUMBER_WITH_PREFIX = 9 + EMPLOYEE_NUMBER;
    protected static final String STUDENT_USERNAME = "niina";
    protected static final String TEACHER_USERNAME = "jukkapalmu";
    protected static final String USERNAME_SUFFIX = "@helsinki.fi";

    @Autowired
    protected CourseService courseService;

    @Autowired
    protected FullSynchronizationJob job;

    @Autowired
    protected MapperService mapperService;

    @Autowired
    protected SynchronizationService synchronizationService;

    @Autowired
    protected SynchronizationJobRunService synchronizationJobRunService;

    @Autowired
    private fi.helsinki.moodi.service.synclock.SyncLockService syncLockService;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private LockedSynchronizationItemMessageBuilder lockedSynchronizationItemMessageBuilder;

    protected void setUpMockServerResponses(String endDate, boolean approved) {
        setupMoodleGetCourseResponse();
        setupOodiCourseUnitRealisationResponse(endDate, approved, false, APPROVED_ENROLLMENT_STATUS_CODE);
    }

    protected void setupOodiCourseUnitRealisationResponse(String endDate,
                                                          boolean approved,
                                                          boolean automaticEnabled,
                                                          int enrollmentStatusCode) {
        expectGetCourseUsersRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                    "/oodi/parameterized-course-realisation.json",
                    new ImmutableMap.Builder()
                        .put("studentnumber", STUDENT_NUMBER)
                        .put("employeeNumber", EMPLOYEE_NUMBER)
                        .put("endDate", endDate)
                        .put("deleted", false)
                        .put("approved", approved)
                        .put("automaticEnabled", automaticEnabled)
                        .put("enrollmentStatusCode", enrollmentStatusCode)
                        .build()),
                MediaType.APPLICATION_JSON));
    }

    protected void setupOodiCourseUnitRealisationResponse(String responseJson) {
        expectGetCourseUsersRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                responseJson,
                new ImmutableMap.Builder()
                    .put("endDate", getFutureDateString())
                    .build()),
                MediaType.APPLICATION_JSON));
    }

    protected void testSynchronizationSummary(SynchronizationType synchronizationType, String moodleResponse, boolean expectErrors) {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, true);

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID, mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId()));

        expectFindUsersRequestsToIAMAndMoodle();

        expectEnrollmentRequestToMoodleWithResponse(moodleResponse,
            new MoodleEnrollment(getTeacherRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), TEACHER_USER_MOODLE_ID, MOODLE_COURSE_ID));

        expectAssignRolesToMoodleWithResponse(moodleResponse, true, new MoodleEnrollment(getStudentRoleId(), STUDENT_USER_MOODLE_ID,
            MOODLE_COURSE_ID));

        SynchronizationSummary summary = synchronizationService.synchronize(synchronizationType);

        SynchronizationItem item =  summary.getItems().get(0);
        List<UserSynchronizationItem> userSynchronizationItems = item.getUserSynchronizationItems();

        UserSynchronizationItemStatus expectedStatus = expectErrors ? UserSynchronizationItemStatus.ERROR : UserSynchronizationItemStatus.SUCCESS;

        assertTrue(userSynchronizationItems.stream().allMatch(userItem -> expectedStatus.equals(userItem.getStatus())));
    }

    protected SynchronizationSummary testSynchronizationSummaryWhenRemovingRoles(
        SynchronizationType synchronizationType,
        ProcessingStatus expectedProcessingStatus,
        String expectedMessage) {

        setUpMockServerResponses(getFutureDateString(), false);

        expectGetEnrollmentsRequestToMoodle(
            MOODLE_COURSE_ID,
            getEnrollmentsResponse(STUDENT_USER_MOODLE_ID, MOODLE_COURSE_ID, mapperService.getStudentRoleId(), mapperService.getMoodiRoleId()));

        expectFindUsersRequestsToIAMAndMoodle();

        SynchronizationSummary summary = synchronizationService.synchronize(synchronizationType);

        SynchronizationItem item = summary.getItems().get(0);

        assertEquals(expectedProcessingStatus, item.getProcessingStatus());

        assertEquals(expectedMessage, item.getProcessingMessage());

        return summary;
    }

    protected void testThatThresholdCheckLocksCourse(String expectedMessage) {
        Mockito.reset(mailSender);

        Course course = findCourse();
        assertFalse(syncLockService.isLocked(course));

        SynchronizationSummary summary = testSynchronizationSummaryWhenRemovingRoles(
            SynchronizationType.FULL,
            ProcessingStatus.LOCKED,
            expectedMessage);
        Mockito.verify(mailSender).send(lockedSynchronizationItemMessageBuilder.buildMessage(summary.getItems()));

        assertTrue(syncLockService.isLocked(course));
    }

    protected void expectFindUsersRequestsToIAMAndMoodle() {
        expectFindStudentRequestToIAMAndMoodle(STUDENT_NUMBER, STUDENT_USERNAME, STUDENT_USER_MOODLE_ID);
        expectFindTeacherRequestToIAMAndMoodle(EMPLOYEE_NUMBER_WITH_PREFIX, TEACHER_USERNAME, TEACHER_USER_MOODLE_ID);
    }

    protected void expectFindStudentRequestToIAMAndMoodle(String studentNumber, String username, int moodleId) {
        expectFindStudentRequestToIAM(studentNumber, username);
        expectGetUserRequestToMoodle(username + USERNAME_SUFFIX, moodleId);
    }

    protected void expectFindTeacherRequestToIAMAndMoodle(String employeeNumber, String username, int moodleId) {
        expectFindEmployeeRequestToIAM(employeeNumber, username);
        expectGetUserRequestToMoodle(username + USERNAME_SUFFIX, moodleId);
    }

    protected Course findCourse() {
        return courseService.findByRealisationId(REALISATION_ID).get();
    }

    protected void assertImportStatus(Course.ImportStatus expectedImportStatus) {
        Course course = findCourse();

        assertEquals(course.importStatus, expectedImportStatus);
    }
}
