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

package fi.helsinki.moodi.service.groupsync;

import fi.helsinki.moodi.integration.moodle.*;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

public class MoodleGroupChangeProcessor {
    private final MoodleService moodleService;
    private final MoodleGrouping sisuCommonGrouping;

    public MoodleGroupChangeProcessor(
        MoodleService moodleService,
        GroupSynchronizationContext context
    ) {
        this.moodleService = moodleService;
        sisuCommonGrouping = moodleService.getOrCreateSisuCommonGrouping(context.getMoodleCourse().id, context.getCourseLanguage());
    }

    public void applyGroupingChanges(@NotNull List<MoodleGroupingChange> changes) {
        for (MoodleGroupingChange change : changes) {
            final Long courseId = change.getMoodleCourseId();
            switch (change.getChangeType()) {
                case CREATE:
                    final MoodleGrouping newGrouping = MoodleGrouping.newMoodleGrouping(
                        courseId,
                        change.getNewName(),
                        change.getSisuStudySubGroupSetLocalId()
                    );
                    final Long newGroupingId = moodleService.createGrouping(newGrouping);
                    applyGroupChanges(courseId, newGroupingId, change.getGroups());
                    change.setStatus(MoodleChangeStatus.APPLIED);
                    break;
                case UPDATE:
                    // TODO: Do we need to update grouping name?
                case KEEP:
                    applyGroupChanges(courseId, change.getMoodleGroupingId(), change.getGroups());
                    change.setStatus(MoodleChangeStatus.APPLIED);
                    break;
                case DELETE:
                    // Delete first the groups in removed grouping
                    applyGroupChanges(courseId, change.getMoodleGroupingId(), change.getGroups());
                    moodleService.deleteGrouping(change.getMoodleGroupingId());
                    change.setStatus(MoodleChangeStatus.APPLIED);
                    break;
                default:
                    break;
            }
        }
    }

    private void applyGroupChanges(@NotNull Long courseId, @NotNull Long groupingId, @NotNull List<MoodleGroupChange> changes) {
        for (MoodleGroupChange change : changes) {
            switch (change.getChangeType()) {
                case CREATE:
                    final MoodleGroup newGroup = MoodleGroup.newGroupFromSisu(
                        courseId,
                        change.getNewNameFromSisu(),
                        null,
                        null,
                        change.getSisuStudySubGroupId()
                    );
                    try {
                        final Long newGroupId = moodleService.createGroup(newGroup, Arrays.asList(groupingId, sisuCommonGrouping.getId()));
                        applyMembershipChanges(newGroupId, change.getMemberships());
                        change.setStatus(MoodleChangeStatus.APPLIED);
                    } catch (Exception e) {
                        change.addError(e.getMessage());
                        change.setStatus(MoodleChangeStatus.FAILED);
                        continue;
                    }
                    break;
                case UPDATE:
                    // TODO: Do we need to sync group name and description?
                case KEEP:
                    applyMembershipChanges(change.getMoodleGroupId(), change.getMemberships());
                    change.setStatus(MoodleChangeStatus.APPLIED);
                    break;
                case DELETE:
                    moodleService.deleteGroup(change.getMoodleGroupId());
                    change.setStatus(MoodleChangeStatus.APPLIED);
                    break;
                default:
                    break;
            }
        }

    }

    private void applyMembershipChanges(@NotNull Long groupId, List<MoodleCourseMembershipChange> changes) {
        final List<Long> newMemberIds = changes.stream()
            .filter(c -> c.getChangeType() == MoodleChangeType.CREATE && c.getErrors().isEmpty())
            .map(MoodleCourseMembershipChange::getMoodleUserId).collect(java.util.stream.Collectors.toList());
        final List<Long> deletedMemberIds = changes.stream()
            .filter(c -> c.getChangeType() == MoodleChangeType.DELETE)
            .map(MoodleCourseMembershipChange::getMoodleUserId).collect(java.util.stream.Collectors.toList());

        if (!newMemberIds.isEmpty()) {
            moodleService.addGroupMembers(groupId, newMemberIds);
        }

        if (!deletedMemberIds.isEmpty()) {
            moodleService.removeGroupMembers(groupId, deletedMemberIds);
        }
        changes.forEach(c -> {
            if (c.getStatus() == MoodleChangeStatus.NOT_APPLIED && c.isMutation()) {
                c.setStatus(MoodleChangeStatus.APPLIED);
            }
        });
    }
}
