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
import fi.helsinki.moodi.integration.moodle.MoodleGrouping;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.sisu.SisuStudyGroupSet;
import fi.helsinki.moodi.integration.sisu.SisuStudySubGroup;
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
public class GroupSynchronizationCreateTest extends AbstractGroupSynchronizationBaseTest {
    @Before public void initialize() {
        moodleCourse = createMoodleFullCourse(moodleCourseId);
        moodleCourseUsers = LongStream.range(1, 100).mapToObj(i -> new MoodleUser(i, "username" + i)).collect(Collectors.toList());

        List<SisuStudySubGroup> sisuStudySubGroups = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            sisuStudySubGroups.add(createSisuStudySubGroup("group" + i, moodleCourseUsers.subList((i - 1) * 20, i * 20).stream()
                .map(MoodleUser::getUsername).collect(Collectors.toList()), false));
        }
        List<SisuStudyGroupSet> sisuStudyGroupSets = Arrays.asList(
            createSisuStudyGroupSet("grouping1", Arrays.asList(sisuStudySubGroups.get(0), sisuStudySubGroups.get(1))),
            createSisuStudyGroupSet("grouping2", Arrays.asList(sisuStudySubGroups.get(2), sisuStudySubGroups.get(3)))
        );

        sisuCourseUnitRealisation = createSisuCourseUnitRealisation(sisuRealisationId, moodleCourseUsers, sisuStudyGroupSets);
        when(courseService.findByRealisationId(sisuRealisationId)).thenReturn(Optional.of(createMoodiCourse(sisuRealisationId, moodleCourseId)));
        when(sisuClient.getCourseUnitRealisation(sisuRealisationId)).thenReturn(Optional.of(sisuCourseUnitRealisation));
        when(moodleService.getCourse(moodleCourseId)).thenReturn(Optional.of(moodleCourse));
        when(moodleService.getGroupingsWithGroups(moodleCourse.id, true)).thenReturn(Collections.emptyList());
        when(moodleService.getEnrolledUsers(moodleCourse.id)).thenReturn(createMoodleUserEnrollments(moodleCourseUsers));
        MoodleGrouping sisuCommonGrouping = createMoodleGrouping(
            3L,
            moodleCourseId,
            Constants.MOODLE_SISU_COMMON_GROUPING_ID,
            Collections.emptyList()
        );
        when(moodleService.getOrCreateSisuCommonGrouping(moodleCourse.id, moodleCourse.lang)).thenReturn(sisuCommonGrouping);
    }

    @Test
    public void testCreateNewGroupsPreview() {
        SynchronizeGroupsResponse response = groupSynchronizationService.preview(sisuRealisationId);
        List<SisuStudyGroupSet> sisuStudyGroupSets = sisuCourseUnitRealisation.studyGroupSets;
        List<MoodleGroupingChange> moodleGroupingChanges = response.getGroupingChanges();
        // Same amount of groupings.
        Assert.assertEquals(sisuStudyGroupSets.size(), moodleGroupingChanges.size());

        for (int i = 0; i < sisuStudyGroupSets.size(); i++) {
            SisuStudyGroupSet sisuStudyGroupSet = sisuStudyGroupSets.get(i);
            MoodleGroupingChange moodleGroupingChange = moodleGroupingChanges.get(i);

            // Correct grouping change type
            Assert.assertEquals(MoodleChangeType.CREATE, moodleGroupingChange.getChangeType());

            List<SisuStudySubGroup> sisuStudySubGroups = sisuStudyGroupSet.getStudySubGroups();
            List<MoodleGroupChange> moodleGroupChanges = moodleGroupingChange.getGroups();

            // Same amount of groups in all groupings.
            Assert.assertEquals(sisuStudySubGroups.size(), moodleGroupChanges.size());
            for (int j = 0; j < sisuCourseUnitRealisation.studyGroupSets.get(i).getStudySubGroups().size(); j++) {
                SisuStudySubGroup sisuStudySubGroup = sisuStudySubGroups.get(j);
                MoodleGroupChange moodleGroupChange = moodleGroupChanges.get(j);

                Assert.assertEquals(MoodleChangeType.CREATE, moodleGroupChange.getChangeType());
                List<String> sisuGroupMemberIds = sisuStudySubGroup.getMemberIds();
                List<MoodleCourseMembershipChange> moodleMembershipChanges = moodleGroupChange.getMemberships();

                // Correct group change type
                Assert.assertEquals(MoodleChangeType.CREATE, moodleGroupChange.getChangeType());
                // Same amount of members in all groups.
                Assert.assertEquals(sisuStudySubGroup.getMemberIds().size(), moodleGroupChange.getMemberships().size());
                // Correct members in all groups.
                for (int k = 0; k < sisuGroupMemberIds.size(); k++) {
                    String sisuGroupMemberId = sisuGroupMemberIds.get(k);
                    MoodleCourseMembershipChange moodleMembershipChange = moodleMembershipChanges.get(k);

                    Assert.assertEquals(sisuGroupMemberId, moodleMembershipChange.getSisuUserId());

                    // Correct membership change type
                    Assert.assertEquals(MoodleChangeType.CREATE, moodleMembershipChange.getChangeType());
                }

            }
            // Correct grouping ids.
            Assert.assertEquals(
                sisuCourseUnitRealisation.studyGroupSets.get(i).getLocalId(),
                response.getGroupingChanges().get(i).getSisuStudySubGroupSetLocalId()
            );
            // Correct grouping prefix in moodle grouping idNumber field.
            Assert.assertEquals(
                Constants.MOODLE_SISU_ID_PREFIX + sisuCourseUnitRealisation.studyGroupSets.get(i).getLocalId(),
                response.getGroupingChanges().get(i).getMoodleIdNumber()
            );
        }
    }

    @Test
    public void testCreateNewGroupsProcess() {
        SynchronizeGroupsResponse response = groupSynchronizationService.process(sisuRealisationId);
        verify(moodleService, times(response.getGroupingChanges().size())).createGrouping(any());
        verify(moodleService, times(response.getGroupingChanges().stream().mapToInt(g -> g.getGroups().size()).sum())).createGroup(any(), any());
    }
}
