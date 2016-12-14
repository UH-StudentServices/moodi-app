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

import fi.helsinki.moodi.integration.esb.EsbService;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiCourseUsers;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;


@TestPropertySource(properties = {"syncTresholds.REMOVE_ROLE.preventAll = false"})
public class SynchronizingProcessorTest extends AbstractMoodiIntegrationTest {

    private static final long MOODLE_USER_ID = 1L;
    private static final long MOODLE_COURSE_ID = 54321L;

    @Autowired
    private SynchronizingProcessor synchronizingProcessor;

    @Autowired
    private CourseService courseService;

    private List<MoodleUserEnrollments> moodleUserEnrollments(MoodleRole... roles) {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();

        moodleUserEnrollments.roles = newArrayList();

        for(MoodleRole role : roles) {
            moodleUserEnrollments.roles.add(role);
        }

        moodleUserEnrollments.id = MOODLE_USER_ID;

        return newArrayList(moodleUserEnrollments);
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
        return new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID);
    }

    private MoodleEnrollment teacherEnrollment() {
        return new MoodleEnrollment(getTeacherRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID);
    }

    private MoodleEnrollment moodiEnrollment() {
        return new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID);
    }

    private MoodleRole createMoodleRole(long roleId) {
        MoodleRole role = new MoodleRole();
        role.roleId = roleId;
        return role;
    }

    /* Add enrollments */

    @Test
    public void studentAndMoodiRolesEnrollment() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, true)
            .withEmptyMoodleEnrollments()
            .expectUserRequestsToESBAndMoodle()
            .expectAddEnrollmentsToMoodleCourse(
                studentEnrollment(),
                moodiEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void teacherAndMoodiRolesEnrollment() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiTeacher(MOODLE_USER_ID)
            .withEmptyMoodleEnrollments()
            .expectUserRequestsToESBAndMoodle()
            .expectAddEnrollmentsToMoodleCourse(
                teacherEnrollment(),
                moodiEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void doesNotAddStudentEnrollmentIfNotApproved() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, false)
            .withEmptyMoodleEnrollments()
            .expectUserRequestsToESBAndMoodle()
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* Remove roles */

    @Test
    public void removeStudentRoleIfNotApproved() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, false)
            .withMoodleEnrollments(moodleUserEnrollments(
                studentRole(),
                moodiRole()))
            .expectUserRequestsToESBAndMoodle()
            .expectAssignRolesToMoodleCourse(
                false,
                studentEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void removeStudentRoleIfNotApprovedAndHasAlsoTeacherRole() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, false)
            .withOodiTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                studentRole(),
                teacherRole(),
                moodiRole()))
            .expectUserRequestsToESBAndMoodle()
            .expectAssignRolesToMoodleCourse(
                false,
                studentEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* Add roles */

    @Test
    public void addStudentRoleIfApproved() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, true)
            .withOodiTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                teacherRole(),
                moodiRole()))
            .expectUserRequestsToESBAndMoodle()
            .expectAssignRolesToMoodleCourse(
                true,
                studentEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void addTeacherRole() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, true)
            .withOodiTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                studentRole(),
                moodiRole()))
            .expectUserRequestsToESBAndMoodle()
            .expectAssignRolesToMoodleCourse(
                true,
                teacherEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* Add moodi role */

    @Test
    public void addMoodiRoleIfStudent() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, true)
            .withMoodleEnrollments(moodleUserEnrollments(
                studentRole()))
            .expectUserRequestsToESBAndMoodle()
            .expectAssignRolesToMoodleCourse(
                true,
                moodiEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void addMoodiRoleIfTeacher() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                teacherRole()))
            .expectUserRequestsToESBAndMoodle()
            .expectAssignRolesToMoodleCourse(
                true,
                moodiEnrollment()
            )
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    /* No action */

    @Test
    public void testNoActionIfAlreadyStudentInMoodle() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiStudent(MOODLE_USER_ID, true)
            .withMoodleEnrollments(moodleUserEnrollments(
                studentRole(),
                moodiRole()))
            .expectUserRequestsToESBAndMoodle()
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    @Test
    public void testNoActionIfAlreadyTeacherInMoodle() {
        SynchronizationItem item = new CourseSynchronizationRequestChain(MOODLE_COURSE_ID)
            .withOodiTeacher(MOODLE_USER_ID)
            .withMoodleEnrollments(moodleUserEnrollments(
                teacherRole(),
                moodiRole()))
            .expectUserRequestsToESBAndMoodle()
            .getSynchronizationItem();

        synchronizingProcessor.doProcess(item);
    }

    private class CourseSynchronizationRequestChain {

        private static final String STUDENT_USERNAME = "studentUsername";
        private static final String TEACHER_USERNAME = "teacherUsername";
        private static final String TEACHER_ID = "1";
        private static final String STUDENT_NUMBER = "1";
        private static final long REALISATION_ID = 12345L;

        private SynchronizationItem synchronizationItem;
        private OodiCourseUsers OodiCourseUsers;

        private Map<Long, OodiStudent> oodiStudentMap = new HashMap<>();
        private Map<Long, OodiTeacher> oodiTeacherMap = new HashMap<>();

        public CourseSynchronizationRequestChain(long moodleCourseId) {
            SynchronizationItem synchronizationItem = new SynchronizationItem(
                courseService.findByRealisationId(REALISATION_ID).get(),
                SynchronizationType.FULL);

            OodiCourseUsers oodiCourseUsers = new OodiCourseUsers();
            oodiCourseUsers.students = newArrayList();
            oodiCourseUsers.teachers = newArrayList();

            SynchronizationItem synchronizationItemWithOodiCourse = synchronizationItem.setOodiCourse(Optional.of(oodiCourseUsers));
            SynchronizationItem synchronizationItemWithMoodleCourse = synchronizationItemWithOodiCourse.setMoodleCourse(Optional.of(createMoodleCourse(moodleCourseId)));

            this.OodiCourseUsers = oodiCourseUsers;
            this.synchronizationItem = synchronizationItemWithMoodleCourse;

        }

        public CourseSynchronizationRequestChain withOodiStudent(long moodleUserId, boolean approved) {
            OodiStudent oodiStudent = new OodiStudent();
            oodiStudent.studentNumber = STUDENT_NUMBER;
            oodiStudent.approved = approved;

            this.OodiCourseUsers.students.add(oodiStudent);
            oodiStudentMap.put(moodleUserId, oodiStudent);

            return this;
        }

        public CourseSynchronizationRequestChain withOodiTeacher(long moodleUserId) {
            OodiTeacher oodiTeacher = new OodiTeacher();
            oodiTeacher.teacherId = TEACHER_ID;

            this.OodiCourseUsers.teachers.add(oodiTeacher);
            oodiTeacherMap.put(moodleUserId, oodiTeacher);

            return this;
        }

        public CourseSynchronizationRequestChain withMoodleEnrollments(List<MoodleUserEnrollments> moodleEnrollments) {
            this.synchronizationItem = this.synchronizationItem.setMoodleEnrollments(Optional.of(moodleEnrollments));
            return this;
        }

        public CourseSynchronizationRequestChain withEmptyMoodleEnrollments() {
            return this.withMoodleEnrollments(newArrayList());
        }

        public CourseSynchronizationRequestChain expectUserRequestsToESBAndMoodle() {
            oodiStudentMap.forEach((moodleUserId, oodiStudent) -> expectFindStudentRequestToEsb(oodiStudent.studentNumber, STUDENT_USERNAME));
            oodiTeacherMap.forEach((moodleUserId, oodiTeacher) -> expectFindEmployeeRequestToEsb(EsbService.TEACHER_ID_PREFIX + oodiTeacher.teacherId, TEACHER_USERNAME));

            oodiStudentMap.forEach((moodleUserId, oodiStudent) -> expectGetUserRequestToMoodle(STUDENT_USERNAME + EsbService.DOMAIN_SUFFIX, moodleUserId));
            oodiTeacherMap.forEach((moodleUserId, oodiTeacher) -> expectGetUserRequestToMoodle(TEACHER_USERNAME + EsbService.DOMAIN_SUFFIX, moodleUserId));

            return this;
        }

        public CourseSynchronizationRequestChain expectAddEnrollmentsToMoodleCourse(MoodleEnrollment... enrollments) {
            expectEnrollmentRequestToMoodle(enrollments);

            return this;
        }

        public CourseSynchronizationRequestChain expectAssignRolesToMoodleCourse(boolean isAssign, MoodleEnrollment... enrollments) {
            expectAssignRolesToMoodle(isAssign, enrollments);

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
