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
import fi.helsinki.moodi.integration.sisu.SisuEnrolment;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static org.junit.Assert.*;

public abstract class AbstractSynchronizationJobTest extends AbstractMoodiIntegrationTest {
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

    protected void setUpMockServerResponses(String endDate, boolean enrolled) {
        setupMoodleGetCourseResponse();
        setupCourseUnitRealisationResponse(endDate, enrolled);
    }

    protected void setupCourseUnitRealisationResponse(String endDate,
                                                      boolean enrolled) {
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(
            Arrays.asList(SISU_REALISATION_IN_DB_ID),
            "/sisu/sisu-course-realisation-in-db.json",
                    new ImmutableMap.Builder()
                        .put("endDate", endDate)
                        .put("enrollmentState", enrolled ? SisuEnrolment.EnrolmentState.ENROLLED : SisuEnrolment.EnrolmentState.PROCESSING)
                        .build());
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList("hy-hlo-4"), "/sisu/persons.json");
    }

    protected void setupCourseUnitRealisationResponseMultiplePeople() {
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(
            Arrays.asList(SISU_REALISATION_IN_DB_ID),
            "/sisu/sisu-course-realisation-in-db-multiple-people.json",
            new ImmutableMap.Builder()
                .put("endDate", getFutureDateString())
                .build()
        );
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList("hy-hlo-1", "hy-hlo-2"), "/sisu/persons-many-1.json");
    }

    protected void testSynchronizationSummary(SynchronizationType synchronizationType, String moodleResponse, boolean expectErrors) {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponses(endDateInFuture, true);

        setupMoodleGetEnrolledUsersForCourses(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA, (int) MOODLE_COURSE_ID_IN_DB,
                    mapperService.getTeacherRoleId(), mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

        expectEnrollmentRequestToMoodleWithResponse(moodleResponse,
            new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_HRAOPE, MOODLE_COURSE_ID_IN_DB));

        expectAssignRolesToMoodleWithResponse(moodleResponse, true, new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID_NIINA,
            MOODLE_COURSE_ID_IN_DB));

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

        setupMoodleGetEnrolledUsersForCourses(
            MOODLE_COURSE_ID_IN_DB,
            Collections.singletonList(
                getMoodleUserEnrollments((int) MOODLE_USER_ID_NIINA, MOODLE_USERNAME_NIINA, (int) MOODLE_COURSE_ID_IN_DB,
                    mapperService.getStudentRoleId(), mapperService.getMoodiRoleId())
            )
        );

        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);

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

    protected void expectFindUsersRequestsToMoodle() {
        expectFindStudentRequestToMoodle(MOODLE_USERNAME_NIINA, MOODLE_USER_ID_NIINA);
        expectFindTeacherRequestToMoodle(MOODLE_USERNAME_HRAOPE, MOODLE_USER_HRAOPE);
    }

    protected void expectFindStudentRequestToMoodle(String username, long moodleId) {
        expectGetUserRequestToMoodle(username, moodleId);
    }

    protected void expectFindTeacherRequestToMoodle(String username, long moodleId) {
        expectGetUserRequestToMoodle(username, moodleId);
    }

    protected Course findCourse() {
        return courseService.findByRealisationId(SISU_REALISATION_IN_DB_ID).get();
    }

    protected void assertImportStatus(Course.ImportStatus expectedImportStatus) {
        Course course = findCourse();

        assertEquals(course.importStatus, expectedImportStatus);
    }
}
