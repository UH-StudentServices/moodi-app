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

package fi.helsinki.moodi.service;

import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessorService;
import fi.helsinki.moodi.service.time.TimeService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ProcessorServiceTest extends AbstractMoodiIntegrationTest {

    // add random 0-1000 millisecond delay to some moodle/sisu mock calls
    private final boolean delayed = false;

    private static final String STUDENT_MOODLE_USERNAME = "studentUsername";
    private static final String TEACHER_MOODLE_USERNAME = "teacherUsername";
    private static final MoodleRole STUDENT_ROLE = new MoodleRole();
    private static final MoodleRole TEACHER_ROLE = new MoodleRole();
    private static final List<StudyRegistryStudent> STUDY_REGISTRY_STUDENTS = new ArrayList<>();
    private static final List<StudyRegistryTeacher> STUDY_REGISTRY_TEACHERS = new ArrayList<>();
    private static final long STUDENT_ROLE_ID = 5;
    private static final long TEACHER_ROLE_ID = 3;
    private static final long SYNCED_ROLE_ID = 10;
    private static final int TEACHER_ID_BASE = 100;
    private static final int MISSING_TEACHER_ID_BASE = 200;

    private final Logger logger = LoggerFactory.getLogger(ProcessorServiceTest.class);

    @Autowired
    private ProcessorService processorService;

    @Autowired
    private TimeService timeService;

    @MockBean
    private CourseRepository courseRepository;

    @Test
    public void thatProcessorsAreRanForEachItem() {
        STUDENT_ROLE.roleId = STUDENT_ROLE_ID;
        TEACHER_ROLE.roleId = TEACHER_ROLE_ID;
        createStudyRegistryStudents();
        createStudyRegistryTeachers();

        List<SynchronizationItem> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            items.add(createFullSynchronizationItem("hy-CUR-" + i, i));
        }

        // Prepare mocks for successful processing of item 0
        SynchronizationItem item = items.get(0);
        item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "success");
        when(courseRepository.findByRealisationId(item.getCourse().realisationId)).thenReturn(Optional.of(item.getCourse()));
        Map<Integer, ProcessingStatus> expectedStatus = new HashMap<>();
        expectedStatus.put(0, ProcessingStatus.SUCCESS);
        List<Integer> expectedUsers = Arrays.asList(0, 1, 2, 3);
        List<Integer> expectedTeachers = Arrays.asList(0, 1);
        expectGetUserRequestsToMoodle(expectedUsers, expectedTeachers, 0);
        expectPostEnrollmentsRequestToMoodle(0, false);

        // Items 1-6 need no mocks because they will be skipped or successfully deleted.
        items.get(1).completeEnrichmentPhase(EnrichmentStatus.IN_PROGRESS, "in progress");
        expectedStatus.put(1, ProcessingStatus.SKIPPED);
        items.get(2).completeEnrichmentPhase(EnrichmentStatus.COURSE_ENDED, "course ended");
        expectedStatus.put(2, ProcessingStatus.SUCCESS);
        items.get(3).completeEnrichmentPhase(EnrichmentStatus.COURSE_NOT_PUBLIC, "course not public");
        expectedStatus.put(3, ProcessingStatus.SUCCESS);
        items.get(4).completeEnrichmentPhase(EnrichmentStatus.MOODLE_COURSE_NOT_FOUND, "moodle course not found");
        expectedStatus.put(4, ProcessingStatus.SUCCESS);
        items.get(5).completeEnrichmentPhase(EnrichmentStatus.ERROR, "error");
        expectedStatus.put(5, ProcessingStatus.SKIPPED);
        items.get(6).completeEnrichmentPhase(EnrichmentStatus.LOCKED, "locked");
        expectedStatus.put(6, ProcessingStatus.SKIPPED);

        // Prepare mocks for successful processing of item 7
        item = items.get(7);
        item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "success");
        when(courseRepository.findByRealisationId(item.getCourse().realisationId)).thenReturn(Optional.of(item.getCourse()));
        expectedStatus.put(7, ProcessingStatus.SUCCESS);
        expectedUsers = Arrays.asList(7, 8, 9, 10);
        expectedTeachers = Arrays.asList(7, 8);
        expectGetUserRequestsToMoodle(expectedUsers, expectedTeachers, 7);
        expectPostEnrollmentsRequestToMoodle(7, false);

        // Prepare mocks for successful processing of item 8
        item = items.get(8);
        item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "success");
        when(courseRepository.findByRealisationId(item.getCourse().realisationId)).thenReturn(Optional.of(item.getCourse()));
        expectedStatus.put(8, ProcessingStatus.SUCCESS);
        // users 8-10 are found cached
        expectedUsers = singletonList(11);
        expectedTeachers = singletonList(9);
        expectGetUserRequestsToMoodle(expectedUsers, expectedTeachers, 8);
        expectPostEnrollmentsRequestToMoodle(8, true);

        // items are grouped by their relevant action and actions are processed in this order:
        // Action.SKIP = ProcessorService.skipItem
        // Action.REMOVE = ProcessorService.removeItem
        // Action.SYNCHRONIZE = ProcessorService.synchronizeItem
        List<SynchronizationItem> processedItems = processorService.process(items);
        // expecting first 3 in result list to be skipped, next 3 to be removed (result is SUCCESS) and last 3 processed
        int[] expectedOrder = {1, 5, 6, 2, 3, 4, 0, 7, 8};
        for (int i = 0; i < 9; i++) {
            item = processedItems.get(i);
            int id = item.getCourse().moodleId.intValue();
            assertEquals(expectedOrder[i], id);
            assertEquals(expectedStatus.get(id), item.getProcessingStatus());
        }
    }

    private void expectPostEnrollmentsRequestToMoodle(int courseId, boolean missingFirst) {
        List<MoodleEnrollment> enrollments = new ArrayList<>();
        // For some reason in last batch missing teacher is posted first.
        if (missingFirst) {
            enrollments.add(new MoodleEnrollment(TEACHER_ROLE_ID, MISSING_TEACHER_ID_BASE + courseId, courseId));
            enrollments.add(new MoodleEnrollment(SYNCED_ROLE_ID, MISSING_TEACHER_ID_BASE + courseId, courseId));
        }
        for (int i = courseId; i < courseId + 4; i++) {
            enrollments.add(new MoodleEnrollment(STUDENT_ROLE_ID, i, courseId));
            enrollments.add(new MoodleEnrollment(SYNCED_ROLE_ID, i, courseId));
        }
        for (int i = courseId + TEACHER_ID_BASE; i < courseId + TEACHER_ID_BASE + 2; i++) {
            enrollments.add(new MoodleEnrollment(TEACHER_ROLE_ID, i, courseId));
            enrollments.add(new MoodleEnrollment(SYNCED_ROLE_ID, i, courseId));
        }
        if (!missingFirst) {
            enrollments.add(new MoodleEnrollment(TEACHER_ROLE_ID, MISSING_TEACHER_ID_BASE + courseId, courseId));
            enrollments.add(new MoodleEnrollment(SYNCED_ROLE_ID, MISSING_TEACHER_ID_BASE + courseId, courseId));
        }
        expectEnrollmentRequestToMoodleWithResponse(EMPTY_RESPONSE, enrollments.toArray(new MoodleEnrollment[0]));
    }

    private void expectGetUserRequestsToMoodle(List<Integer> expectedStudents, List<Integer> expectedTeachers, int missingId) {
        expectedStudents.forEach(i -> {
                StudyRegistryStudent student = STUDY_REGISTRY_STUDENTS.get(i);
                expectGetUserRequestToMoodle(student.userName, i, delayed);
            }
        );
        expectedTeachers.forEach(i -> {
                StudyRegistryTeacher teacher = STUDY_REGISTRY_TEACHERS.get(i);
                expectGetUserRequestToMoodle(teacher.userName, TEACHER_ID_BASE + i, delayed);
            }
        );
        expectGetUserRequestToMoodle("missing_creator" + missingId + "@test.fi", MISSING_TEACHER_ID_BASE + missingId, delayed);
    }

    private void createStudyRegistryStudents() {
        STUDY_REGISTRY_STUDENTS.clear();
        for (int i = 0; i < 15; i++) {
            StudyRegistryStudent student = new StudyRegistryStudent();
            student.studentNumber = "123000" + i;
            student.userName = "student" + i + "@test.fi";
            student.firstNames = "firstname" + i;
            student.lastName = "lastname" + i;
            student.isEnrolled = true;
            STUDY_REGISTRY_STUDENTS.add(student); // core_user_get_users_by_field
        }
    }

    private void createStudyRegistryTeachers() {
        STUDY_REGISTRY_TEACHERS.clear();
        for (int i = 0; i < 15; i++) {
            StudyRegistryTeacher teacher = new StudyRegistryTeacher();
            teacher.employeeNumber = "E500" + i;
            teacher.userName = "teacher" + i + "@test.fi";
            teacher.firstNames = "firstname" + i;
            teacher.lastName = "lastname" + i;
            STUDY_REGISTRY_TEACHERS.add(teacher);
        }
    }

    private SynchronizationItem createFullSynchronizationItem(String realisationId, int i) {
        Course course = new Course();
        course.realisationId = realisationId;
        course.importStatus = Course.ImportStatus.IN_PROGRESS;
        course.moodleId = (long) i;
        course.created = timeService.getCurrentUTCDateTime();
        course.modified = timeService.getCurrentUTCDateTime();
        course.creatorUsername = "missing_creator" + i + "@test.fi";
        course.removed = false;
        MoodleFullCourse moodleCourse = new MoodleFullCourse();
        moodleCourse.id = (long) i;
        moodleCourse.idNumber = "" + i;
        moodleCourse.shortName = "mdl" + i;
        moodleCourse.fullName = "moodleCourse" + i;
        StudyRegistryCourseUnitRealisation cur = new StudyRegistryCourseUnitRealisation();
        cur.realisationId = realisationId;
        cur.realisationName = "course " + realisationId;
        cur.students = STUDY_REGISTRY_STUDENTS.subList(i, i + 4);
        cur.teachers = STUDY_REGISTRY_TEACHERS.subList(i, i + 2);
        SynchronizationItem item = new SynchronizationItem(course, SynchronizationType.FULL);
        item.setStudyRegistryCourse(cur);
        item.setMoodleCourse(moodleCourse);
        item.setMoodleEnrollments(createExistingMoodleUserEnrollments());
        return item;
    }

    private MoodleUserEnrollments createStudentEnrollment() {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.username = STUDENT_MOODLE_USERNAME;
        moodleUserEnrollments.roles = singletonList(STUDENT_ROLE);
        return moodleUserEnrollments;
    }

    private MoodleUserEnrollments createTeacherEnrollment() {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.username = TEACHER_MOODLE_USERNAME;
        moodleUserEnrollments.roles = singletonList(TEACHER_ROLE);
        return moodleUserEnrollments;
    }

    private List<MoodleUserEnrollments> createExistingMoodleUserEnrollments() {
        List<MoodleUserEnrollments> enrollments = new ArrayList<>();
        enrollments.add(createStudentEnrollment());
        enrollments.add(createTeacherEnrollment());
        return enrollments;
    }
}
