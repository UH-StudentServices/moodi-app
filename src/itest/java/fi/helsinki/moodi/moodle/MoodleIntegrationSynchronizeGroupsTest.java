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

package fi.helsinki.moodi.moodle;

import fi.helsinki.moodi.service.groupsync.GroupSynchronizationService;
import fi.helsinki.moodi.service.groupsync.MoodleChangeStatus;
import fi.helsinki.moodi.service.groupsync.MoodleChangeType;
import fi.helsinki.moodi.service.groupsync.SynchronizeGroupsResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertTrue;

public class MoodleIntegrationSynchronizeGroupsTest extends AbstractMoodleIntegrationTest {

    @Autowired
    private GroupSynchronizationService groupSynchronizationService;

    @Test
    public void testMoodleIntegrationWhenSynchronizingGroups() {
        String sisuCourseId = getSisuCourseId();

        expectCreator(creatorUser);

        expectCourseRealisationsWithUsers(
            sisuCourseId,
            newArrayList(studentUser, studentUserNotInMoodle),
            newArrayList(teacherUser)
        );

        importCourse(sisuCourseId, creatorUser.personId);

        SynchronizeGroupsResponse response = groupSynchronizationService.process(sisuCourseId);
        assertTrue(response.getGroupingChanges().stream().noneMatch(g -> g.getStatus() == MoodleChangeStatus.FAILED));
        assertTrue(response.getGroupingChanges().stream().anyMatch(g -> g.getChangeType() == MoodleChangeType.CREATE));

        resetAndExpectCourseRealisationsWithRemovedSubGroupSet(
            sisuCourseId,
            newArrayList(),
            newArrayList()
        );

        SynchronizeGroupsResponse responseRemovedGrouping = groupSynchronizationService.process(sisuCourseId);
        assertTrue(responseRemovedGrouping.getGroupingChanges().stream().noneMatch(g -> g.getStatus() == MoodleChangeStatus.FAILED));
        assertTrue(responseRemovedGrouping.getGroupingChanges().stream().anyMatch(g -> g.getChangeType() == MoodleChangeType.DELETE));

    }
}
