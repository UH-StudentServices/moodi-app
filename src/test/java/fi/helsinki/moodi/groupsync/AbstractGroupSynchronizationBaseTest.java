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

package fi.helsinki.moodi.groupsync;

import fi.helsinki.moodi.integration.moodle.*;
import fi.helsinki.moodi.integration.sisu.*;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.groupsync.GroupSynchronizationService;
import org.junit.Rule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public abstract class AbstractGroupSynchronizationBaseTest {
    protected final String sisuRealisationId = "test";
    protected final long moodleCourseId = 123L;
    //Creating new rule with recommended Strictness setting
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);
    @Mock
    protected CourseService courseService;
    @Mock
    protected MoodleService moodleService;
    @Mock
    protected SisuClient sisuClient;
    @InjectMocks
    protected GroupSynchronizationService groupSynchronizationService;
    protected MoodleFullCourse moodleCourse;
    protected SisuCourseUnitRealisation sisuCourseUnitRealisation;
    protected List<MoodleUser> moodleCourseUsers;
    protected List<MoodleGrouping> moodleGroupings;

    protected List<MoodleUserEnrollments> createMoodleUserEnrollments(List<MoodleUser> users) {
        List<MoodleUserEnrollments> enrollments = new ArrayList<>();
        for (MoodleUser user : users) {
            MoodleUserEnrollments userEnrollments = new MoodleUserEnrollments();
            userEnrollments.username = user.getUsername();
            userEnrollments.id = user.getId();
            enrollments.add(userEnrollments);
        }
        return enrollments;
    }

    protected Course createMoodiCourse(String sisuRealisationId, long moodleCourseId) {
        Course course = new Course();
        course.id = moodleCourseId + 1;
        course.realisationId = sisuRealisationId;
        course.moodleId = moodleCourseId;
        course.importStatus = Course.ImportStatus.COMPLETED;
        course.removed = false;
        return course;
    }

    protected MoodleFullCourse createMoodleFullCourse(long id) {
        MoodleFullCourse course = new MoodleFullCourse();
        course.id = id;
        course.lang = SisuLocale.FI.toString();
        return course;
    }

    protected MoodleGroupData createMoodleGroupData(long id, long moodleCourseId, String idNumber, List<MoodleUser> users) {
        return new MoodleGroupData(
            id,
            moodleCourseId,
            "name " + id,
            "description " + id,
            null,
            null,
            idNumber,
            users
        );
    }

    protected MoodleGrouping createMoodleGrouping(long id, long moodleCourseId, String idNumber, List<MoodleGroupData> groups) {
        return MoodleGrouping.fromData(new MoodleGroupingData(
            id,
            moodleCourseId,
            "name " + id,
            "description " + id,
            null,
            idNumber,
            groups
        ));
    }

    protected SisuCourseUnitRealisation createSisuCourseUnitRealisation(
        String id,
        List<MoodleUser> users,
        List<SisuStudyGroupSet> sisuStudyGroupSets) {
        SisuCourseUnitRealisation sisuCourseUnitRealisation = new SisuCourseUnitRealisation();
        sisuCourseUnitRealisation.id = id;
        sisuCourseUnitRealisation.studyGroupSets = sisuStudyGroupSets;
        return sisuCourseUnitRealisation;
    }

    protected SisuStudyGroupSet createSisuStudyGroupSet(String id, List<SisuStudySubGroup> sisuStudyGroups) {
        return new SisuStudyGroupSet(id,
            new SisuLocalisedValue(
                "fi groupset " + id,
                "sv groupset " + id,
                "en groupset " + id
            ),
            sisuStudyGroups
        );
    }

    protected SisuStudySubGroup createSisuStudySubGroup(String id, List<String> memberIds) {
        return new SisuStudySubGroup(id, new SisuLocalisedValue(
            "fi group " + id,
            "sv group " + id,
            "en group " + id
        ), memberIds);
    }
}
