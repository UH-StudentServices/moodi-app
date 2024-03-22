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

import fi.helsinki.moodi.Constants;
import fi.helsinki.moodi.integration.moodle.*;
import fi.helsinki.moodi.integration.sisu.*;
import fi.helsinki.moodi.service.groupsync.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.mockito.Mockito.*;

/**
 * Tests for creating groups to Moodle course from Sisu course unit realisation
 * without existing groups in Moodle.
 */
public class GroupSynchronizationUpdateTest extends AbstractGroupSynchronizationBaseTest {

    @Before public void initialize() {
        moodleCourse = createMoodleFullCourse(moodleCourseId);
        moodleCourseUsers = LongStream.range(1, 100).mapToObj(i -> new MoodleUser(i, "username" + i)).collect(Collectors.toList());
        // 5 groups with 10 members
        List<MoodleGroupData> moodleGroups = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            moodleGroups.add(createMoodleGroupData(i, moodleCourseId, "sisu:group" + i, moodleCourseUsers.subList((i - 1) * 10, i * 10)));
        }
        // group for removing cancelled group
        moodleGroups.add(createMoodleGroupData(6, moodleCourseId, "sisu:group6", moodleCourseUsers));
        // 3 groupings with groups and common grouping
        moodleGroupings = Arrays.asList(
            createMoodleGrouping(1L, moodleCourseId, "sisu:grouping1", Arrays.asList(moodleGroups.get(0), moodleGroups.get(1))),
            createMoodleGrouping(2L, moodleCourseId, "sisu:grouping2", Arrays.asList(moodleGroups.get(2), moodleGroups.get(3), moodleGroups.get(5))),
            createMoodleGrouping(3L, moodleCourseId, "sisu:grouping3", Collections.singletonList(moodleGroups.get(4))),
            createMoodleGrouping(4L, moodleCourseId, Constants.MOODLE_SISU_COMMON_GROUPING_ID, moodleGroups)
        );

        // group1 is removed.
        List<SisuStudySubGroup> sisuStudySubGroups = new ArrayList<>();
        for (int i = 2; i <= 4; i++) {
            sisuStudySubGroups.add(createSisuStudySubGroup("group" + i, moodleCourseUsers.subList((i - 1) * 10, i * 10)
                .stream().map(MoodleUser::getUsername).collect(Collectors.toList()), false));
        }
        // cancelled group, which is removed
        sisuStudySubGroups.add(createSisuStudySubGroup("group6", moodleCourseUsers.subList(50, 60)
            .stream().map(MoodleUser::getUsername).collect(Collectors.toList()), true));
        // cancelled group, which should not be added to Moodle
        sisuStudySubGroups.add(createSisuStudySubGroup("group7", moodleCourseUsers.subList(60, 70)
            .stream().map(MoodleUser::getUsername).collect(Collectors.toList()), true));

        // grouping3 is removed. group1 is removed from grouping1.
        List<SisuStudyGroupSet> sisuStudyGroupSets = Arrays.asList(
            createSisuStudyGroupSet("grouping1", Arrays.asList(sisuStudySubGroups.get(0), sisuStudySubGroups.get(4))),
            createSisuStudyGroupSet("grouping2", Arrays.asList(sisuStudySubGroups.get(1), sisuStudySubGroups.get(2), sisuStudySubGroups.get(3)))
        );

        sisuCourseUnitRealisation = createSisuCourseUnitRealisation(sisuRealisationId, moodleCourseUsers, sisuStudyGroupSets);
        when(courseService.findByRealisationId(sisuRealisationId)).thenReturn(Optional.of(createMoodiCourse(sisuRealisationId, moodleCourseId)));
        when(sisuClient.getCourseUnitRealisation(sisuRealisationId)).thenReturn(Optional.of(sisuCourseUnitRealisation));
        when(moodleService.getCourse(moodleCourseId)).thenReturn(Optional.of(moodleCourse));
        when(moodleService.getGroupingsWithGroups(moodleCourse.id, true)).thenReturn(moodleGroupings);
        when(moodleService.getCourse(moodleCourseId)).thenReturn(Optional.of(moodleCourse));
        when(moodleService.getEnrolledUsers(moodleCourse.id)).thenReturn(createMoodleUserEnrollments(moodleCourseUsers));
        when(moodleService.getOrCreateSisuCommonGrouping(moodleCourse.id, moodleCourse.lang)).thenReturn(moodleGroupings.get(3));
    }

    @Test
    public void testUpdateGroupsPreview() {
        SynchronizeGroupsResponse response = groupSynchronizationService.preview(sisuRealisationId);
        List<MoodleGroupingChange> moodleGroupingChanges = response.getGroupingChanges();
        // grouping3 is marked to be deleted
        Assert.assertEquals(MoodleChangeType.DELETE, moodleGroupingChanges.stream()
            .filter(g -> g.getMoodleIdNumber().equals("sisu:grouping3")).findFirst().get().getChangeType());
        // group1 is marked to be deleted
        Assert.assertEquals(MoodleChangeType.DELETE,
            moodleGroupingChanges.stream().filter(g -> g
                    .getMoodleIdNumber().equals("sisu:grouping1"))
                .findFirst().get().getGroups().stream()
                .filter(g -> g.getMoodleIdNumber().equals("sisu:group1"))
                .findFirst().get().getChangeType());
        // group6 is marked to be deleted
        Assert.assertEquals(MoodleChangeType.DELETE,
            moodleGroupingChanges.stream().filter(g -> g
                    .getMoodleIdNumber().equals("sisu:grouping2"))
                .findFirst().get().getGroups().stream()
                .filter(g -> g.getMoodleIdNumber().equals("sisu:group6"))
                .findFirst().get().getChangeType());
        // group7 is not included to changes
        Assert.assertEquals(0,
            moodleGroupingChanges.stream().filter(g -> g
                    .getMoodleIdNumber().equals("sisu:grouping1"))
                .findFirst().get().getGroups().stream()
                .filter(g -> g.getMoodleIdNumber().equals("sisu:group7"))
                .count());
    }

    @Test
    public void testUpdateGroupsProcess() {
        SynchronizeGroupsResponse response = groupSynchronizationService.process(sisuRealisationId);
        verify(moodleService, times(3)).deleteGroup(any());
        verify(moodleService, times(1)).deleteGrouping(any());
    }

}
