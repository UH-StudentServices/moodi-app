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

import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


public class SynchronizingProcessorTest extends AbstractMoodiIntegrationTest {

    private static final long MOODLE_COURSE_ID = 54321L;
    private static final long MOODLE_USER_ID = 1L;
    private static final long REALISATION_ID = 12345L;

    @Autowired
    private SynchronizingProcessor synchronizingProcessor;

    @Autowired
    private CourseService courseService;

    public OodiCourseUnitRealisation createOodiCourse() {
        OodiStudent oodiStudent = new OodiStudent();
        oodiStudent.studentNumber = "1";

        OodiTeacher oodiTeacher = new OodiTeacher();
        oodiTeacher.teacherId = "1";

        OodiCourseUnitRealisation oodiCourseUnitRealisation = new OodiCourseUnitRealisation();
        oodiCourseUnitRealisation.students = Lists.newArrayList(oodiStudent);
        oodiCourseUnitRealisation.teachers = Lists.newArrayList();

        return oodiCourseUnitRealisation;
    }

    public MoodleFullCourse createMoodleCourse() {
        MoodleFullCourse moodleFullCourse = new MoodleFullCourse();
        moodleFullCourse.id = MOODLE_COURSE_ID;
        return moodleFullCourse;
    }

    public List<MoodleUserEnrollments> creatMoodleUserEnrollmentsWithoutRole() {
        return Lists.newArrayList();
    }

    public List<MoodleUserEnrollments> creatMoodleUserEnrollmentsWithRole() {
        MoodleRole moodleRole = new MoodleRole();
        moodleRole.roleId = getTeacherRoleId();

        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.roles = Lists.newArrayList(moodleRole);
        moodleUserEnrollments.id = MOODLE_USER_ID;

        return Lists.newArrayList(moodleUserEnrollments);
    }

    private void expectGetStudentNumber() {
        esbMockServer.expect(requestTo("https://esbmt1.it.helsinki.fi/iam/findStudent/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("[{\"studentNumber\": \"10\", \"username\" : \"mag_simp\"}]", MediaType.APPLICATION_JSON));
    }

    private void expectGetMoodleUser() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json&field=username&values%5B0%5D=mag_simp%40helsinki.fi"))
            .andRespond(withSuccess(String.format("[{\"username\" : \"mag_simp\", \"id\" : \"%s\"}]", MOODLE_USER_ID), MediaType.APPLICATION_JSON));
    }

    private void expectNewEnrollmentToMoodle() {
        expectEnrollmentRequestToMoodle(
            new MoodleEnrollment(getStudentRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID),
            new MoodleEnrollment(getMoodiRoleId(), MOODLE_USER_ID, MOODLE_COURSE_ID));
    }

    private void expectRoleAssignToMoodle() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(
                "wstoken=xxxx1234&wsfunction=core_role_assign_roles&moodlewsrestformat=json&assignments%5B0%5D%5Buserid%5D=1&assignments%5B0%5D%5Broleid%5D="
                    + getStudentRoleId()
                    + "&assignments%5B0%5D%5Binstanceid%5D="
                    + MOODLE_COURSE_ID
                    + "&assignments%5B0%5D%5Bcontextlevel%5D=course"))
            .andRespond(withSuccess());
    }

    @Test
    public void testSynchronizeStudentNotInMoodle() {
        expectGetStudentNumber();
        expectGetMoodleUser();
        expectNewEnrollmentToMoodle();

        SynchronizationItem synchronizationItem = new SynchronizationItem(getCourse());
        SynchronizationItem synchronizationItemWithMoodleEnrollments = synchronizationItem.setMoodleEnrollments(Optional.of((creatMoodleUserEnrollmentsWithoutRole())));
        SynchronizationItem synchronizationItemWithOodiCourse = synchronizationItemWithMoodleEnrollments.setOodiCourse(Optional.of(createOodiCourse()));
        SynchronizationItem synchronizationItemWithMoodleCourse = synchronizationItemWithOodiCourse.setMoodleCourse(Optional.of(createMoodleCourse()));

        synchronizingProcessor.doProcess(synchronizationItemWithMoodleCourse);
    }

    @Test
    public void testSynchronizeStudentAlreadyInMoodleWithDifferentRole() {
        expectGetStudentNumber();
        expectGetMoodleUser();
        expectRoleAssignToMoodle();

        SynchronizationItem synchronizationItem = new SynchronizationItem(getCourse());
        SynchronizationItem synchronizationItemWithMoodleEnrollments = synchronizationItem.setMoodleEnrollments(Optional.of((creatMoodleUserEnrollmentsWithRole())));
        SynchronizationItem synchronizationItemWithOodiCourse = synchronizationItemWithMoodleEnrollments.setOodiCourse(Optional.of(createOodiCourse()));
        SynchronizationItem synchronizationItemWithMoodleCourse = synchronizationItemWithOodiCourse.setMoodleCourse(Optional.of(createMoodleCourse()));

        synchronizingProcessor.doProcess(synchronizationItemWithMoodleCourse);
    }

    private Course getCourse() {
        return courseService.findByRealisationId(REALISATION_ID).get();
    }


}
