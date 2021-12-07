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

package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.integration.moodle.MoodleCourseData;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;

@TestPropertySource(properties = {
    "syncTresholds.SUSPEND_ENROLLMENT.preventAll = 0"
})
public class SynchronizingProcessorTest extends AbstractMoodiIntegrationTest {

    private static final long MOODLE_USER_ID = 1L;

    @Autowired
    private SynchronizingProcessor synchronizingProcessor;

    @Autowired
    private CourseService courseService;

    private List<MoodleUserEnrollments> moodleUserEnrollments(long moodleCourseId, MoodleRole... roles) {
        return newArrayList(moodleUserEnrollmentsForUser(moodleCourseId, MOODLE_USER_ID, "studentUsername@helsinki.fi", roles));
    }

    private MoodleUserEnrollments moodleUserEnrollmentsForUser(long moodleCourseId, long moodleUserId, String userName, MoodleRole... roles) {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();

        moodleUserEnrollments.roles = newArrayList();

        moodleUserEnrollments.roles.addAll(Arrays.asList(roles));

        moodleUserEnrollments.id = moodleUserId;
        moodleUserEnrollments.username = userName;
        moodleUserEnrollments.enrolledCourses = Arrays.asList(new MoodleCourseData(moodleCourseId));

        return moodleUserEnrollments;
    }

    private MoodleRole teacherRole() {
        return createMoodleRole(getTeacherRoleId());
    }

    private MoodleRole studentRole() {
        return createMoodleRole(getStudentRoleId());
    }

    private MoodleRole moodiRole() {
        return createMoodleRole(getMoodiRoleId());
    }

    private MoodleEnrollment studentEnrollment() {
        return new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID_IN_DB);
    }

    private MoodleEnrollment teacherEnrollment() {
        return new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID_IN_DB);
    }

    private MoodleEnrollment moodiEnrollment() {
        return new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID_IN_DB);
    }

    private MoodleRole createMoodleRole(long roleId) {
        MoodleRole role = new MoodleRole();
        role.roleId = roleId;
        return role;
    }

    /* Add enrollments */

    @Test
    public void thatUserIsEnrolledWithStudentAndMoodiRoles() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, true)
            .withEmptyMoodleEnrollments()
            .expectUserRequestsToMoodle()
            .expectAddEnrollmentsToMoodleCourse(
                studentEnrollment(),
                moodiEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatUserIsEnrolledWithTeacherAndMoodiRoles() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withTeacher(MOODLE_USER_ID)
            .withEmptyMoodleEnrollments()
            .expectUserRequestsToMoodle()
            .expectAddEnrollmentsToMoodleCourse(
                teacherEnrollment(),
                moodiEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatCreatorIsEnrolledWithTeacherAndMoodiRoles() {
        String creatorUsername = "jotain@helsinki.fi";
        expectGetUserRequestToMoodle(creatorUsername, MOODLE_USER_ID);
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withCreator(creatorUsername)
            .withEmptyMoodleEnrollments()
            .expectAddEnrollmentsToMoodleCourse(
                teacherEnrollment(),
                moodiEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatUserIsNotEnrolledWithOnlyMoodiRoleIfNotApproved() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, false)
            .withEmptyMoodleEnrollments()
            .expectUserRequestsToMoodle()
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* Remove roles */

    @Test
    public void thatStudentRoleIsRemovedIfNotEnrolled() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, false)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                studentRole(),
                teacherRole(),
                moodiRole()))
            .expectUserRequestsToMoodle()
            .expectUnAssignRolesToMoodleCourse(studentEnrollment())
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatStudentIsSuspendedIfNotEnrolled() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, false)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                studentRole(),
                moodiRole()))
            .expectUserRequestsToMoodle()
            .expectSuspendsToMoodleCourse(
                studentEnrollment()
            )
            .expectUnAssignRolesToMoodleCourse(studentEnrollment())
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatStudentWithSyncedRoleIsSuspendedIfNotPresentInStudyRegistry() {
        // Should get suspended, as has synced role and is not in Sisu.
        MoodleUserEnrollments studentWithSyncedRole =
            moodleUserEnrollmentsForUser(MOODLE_COURSE_ID_IN_DB, MOODLE_USER_ID, "user1@helsinki.fi", studentRole(), moodiRole());
        // Should not get suspended, because does not have synced role.
        MoodleUserEnrollments studentWithoutSyncedRole =
            moodleUserEnrollmentsForUser(MOODLE_COURSE_ID_IN_DB, 2, "user2@helsinki.fi", studentRole());
        // Should not get suspended, because is a teacher.
        MoodleUserEnrollments teacherWithSyncedRole =
            moodleUserEnrollmentsForUser(MOODLE_COURSE_ID_IN_DB, 3, "user3@helsinki.fi", teacherRole(), moodiRole());
        // Should not get suspended, because is in Sisu.
        MoodleUserEnrollments studentInSisu =
            moodleUserEnrollmentsForUser(MOODLE_COURSE_ID_IN_DB, 4, CourseSynchronizationRequestChain.STUDENT_USERNAME, studentRole(), moodiRole());

        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(4, true)
            .withMoodleEnrollments(Arrays.asList(
                studentWithSyncedRole,
                studentWithoutSyncedRole,
                teacherWithSyncedRole,
                studentInSisu
            ))
            .expectUserRequestsToMoodle(CourseSynchronizationRequestChain.STUDENT_USERNAME)
            // Only one suspend and role de-assignment: for studentWithSyncedRole
            .expectSuspendsToMoodleCourse(
                studentEnrollment()
            )
            .expectUnAssignRolesToMoodleCourse(studentEnrollment())
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* Add roles */

    @Test
    public void thatStudentRoleIsAddedIfEnrolled() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, true)
            .withTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                teacherRole(),
                moodiRole()))
            .expectUserRequestsToMoodle()
            .expectAssignRolesToMoodleCourse(studentEnrollment())
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatTeacherRoleIsAdded() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, true)
            .withTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                studentRole(),
                moodiRole()))
            .expectUserRequestsToMoodle()
            .expectAssignRolesToMoodleCourse(teacherEnrollment())
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* Add moodi role */

    @Test
    public void thatMoodiRoleIsAddedForStudent() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, true)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                studentRole()))
            .expectUserRequestsToMoodle()
            .expectAssignRolesToMoodleCourse(moodiEnrollment())
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatMoodiRoleIsAddedForTeacher() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                teacherRole()))
            .expectUserRequestsToMoodle()
            .expectAssignRolesToMoodleCourse(moodiEnrollment())
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* No action */

    @Test
    public void thatNoActionIsTakenIfStudentAlreadyHasCorrectRoles() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withStudent(MOODLE_USER_ID, true)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                studentRole(),
                moodiRole()))
            .expectUserRequestsToMoodle()
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void thatNoActionIsTakenIfTeacherAlreadyHasCorrectRoles() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID_IN_DB)
            .withTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                MOODLE_COURSE_ID_IN_DB,
                teacherRole(),
                moodiRole()))
            .expectUserRequestsToMoodle()
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    private class CourseSynchronizationRequestChain {

        private static final String STUDENT_USERNAME = "studentUsername@helsinki.fi";
        private static final String TEACHER_USERNAME = "teacherUsername@helsinki.fi";
        private static final String TEACHER_ID = "1";
        private static final String STUDENT_NUMBER = "1";

        private SynchronizationItem synchronizationItem;
        private StudyRegistryCourseUnitRealisation courseUnitRealisation;

        private Map<Long, StudyRegistryStudent> studentMap = new HashMap<>();
        private Map<Long, StudyRegistryTeacher> teacherMap = new HashMap<>();

        public CourseSynchronizationRequestChain(long moodleCourseId) {
            SynchronizationItem synchronizationItem = new SynchronizationItem(
                courseService.findByRealisationId(SISU_REALISATION_IN_DB_ID).get(),
                SynchronizationType.FULL);

            StudyRegistryCourseUnitRealisation cur = new StudyRegistryCourseUnitRealisation();
            cur.students = newArrayList();
            cur.teachers = newArrayList();

            SynchronizationItem synchronizationItemWithSisuCourse = synchronizationItem.setStudyRegistryCourse(Optional.of(cur));
            SynchronizationItem synchronizationItemWithMoodleCourse = synchronizationItemWithSisuCourse
                .setMoodleCourse(Optional.of(createMoodleCourse(moodleCourseId)));

            this.courseUnitRealisation = cur;
            this.synchronizationItem = synchronizationItemWithMoodleCourse;

        }

        public CourseSynchronizationRequestChain withStudent(long moodleUserId,
                                                             boolean enrolled) {
            StudyRegistryStudent student = new StudyRegistryStudent();
            student.studentNumber = STUDENT_NUMBER;
            student.isEnrolled = enrolled;
            student.userName = STUDENT_USERNAME;

            this.courseUnitRealisation.students.add(student);
            studentMap.put(moodleUserId, student);

            return this;
        }

        public CourseSynchronizationRequestChain withTeacher(long moodleUserId) {
            StudyRegistryTeacher teacher = new StudyRegistryTeacher();
            teacher.employeeNumber = TEACHER_ID;
            teacher.userName = TEACHER_USERNAME;

            this.courseUnitRealisation.teachers.add(teacher);
            teacherMap.put(moodleUserId, teacher);

            return this;
        }

        public CourseSynchronizationRequestChain withCreator(String creatorUsername) {
            this.synchronizationItem.getCourse().creatorUsername = creatorUsername;
            return this;
        }

        public CourseSynchronizationRequestChain withMoodleEnrollments(List<MoodleUserEnrollments> moodleEnrollments) {
            this.synchronizationItem = this.synchronizationItem.setMoodleEnrollments(Optional.of(moodleEnrollments));
            return this;
        }

        public CourseSynchronizationRequestChain withEmptyMoodleEnrollments() {
            return this.withMoodleEnrollments(newArrayList());
        }

        public CourseSynchronizationRequestChain expectUserRequestsToMoodle(String studentUserName) {
            studentMap.forEach((moodleUserId, sisuStudent) -> expectGetUserRequestToMoodle(
                studentUserName, moodleUserId));
            teacherMap.forEach((moodleUserId, sisuTeacher) -> expectGetUserRequestToMoodle(
                TEACHER_USERNAME, moodleUserId));

            return this;
        }

        public CourseSynchronizationRequestChain expectUserRequestsToMoodle() {
            return expectUserRequestsToMoodle(STUDENT_USERNAME);
        }

        public CourseSynchronizationRequestChain expectAddEnrollmentsToMoodleCourse(MoodleEnrollment... enrollments) {
            expectEnrollmentRequestToMoodle(enrollments);

            return this;
        }

        public CourseSynchronizationRequestChain expectSuspendsToMoodleCourse(MoodleEnrollment... enrollments) {
            expectSuspendRequestToMoodle(enrollments);
            return this;
        }

        public CourseSynchronizationRequestChain expectAssignRolesToMoodleCourse(MoodleEnrollment... enrollments) {
            expectAssignRolesToMoodle(true, enrollments);

            return this;
        }

        public CourseSynchronizationRequestChain expectUnAssignRolesToMoodleCourse(MoodleEnrollment... enrollments) {
            expectAssignRolesToMoodle(false, enrollments);

            return this;
        }

        public SynchronizationItem getSynchronizationItem() {
            return this.synchronizationItem;
        }

        private MoodleFullCourse createMoodleCourse(long moodleCourseId) {
            MoodleFullCourse moodleFullCourse = new MoodleFullCourse();
            moodleFullCourse.id = moodleCourseId;
            return moodleFullCourse;
        }

    }

}
