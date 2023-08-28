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

import fi.helsinki.moodi.integration.moodle.MoodleGroup;
import fi.helsinki.moodi.integration.moodle.MoodleGrouping;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.sisu.SisuLocale;
import fi.helsinki.moodi.integration.sisu.SisuLocalisedValue;
import fi.helsinki.moodi.integration.sisu.SisuStudyGroupSet;
import fi.helsinki.moodi.integration.sisu.SisuStudySubGroup;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class builds changes for Moodle groupings, groups and group memberships based on Sisu study group sets and subgroups recursively.
 */
@AllArgsConstructor
public class MoodleGroupChangeBuilder {
    private GroupSynchronizationContext context;
    // Moodle doesn't allow duplicate group & grouping names, so whe add running sequence to already existing group names.
    private Map<String, Long> groupNames;
    private Map<String, Long> groupingNames;

    private static boolean isValidStudyGroupSet(SisuStudyGroupSet studyGroupSet) {
        // StudyGroupSet needs to have at least two subgroups to be synced to Moodle due to fact that Sisu requires
        // cur to have at least one StudyGroupSet with one StudySubGroup always.
        return studyGroupSet.getStudySubGroups() != null && studyGroupSet.getStudySubGroups().size() > 1;
    }

    public static List<MoodleGroupingChange> buildChanges(
        GroupSynchronizationContext context
    ) {
        Map<String, Long> groupNames = context.getMoodleGroupingsWithGroups().stream()
            .flatMap(g -> g.getGroups().stream())
            .map(MoodleGroup::getName)
            .collect(java.util.stream.Collectors.toMap(
                name -> name,
                name -> 1L,
                Long::sum
            ));
        Map<String, Long> groupingNames = context.getMoodleGroupingsWithGroups().stream()
            .map(MoodleGrouping::getName)
            .collect(java.util.stream.Collectors.toMap(
                name -> name,
                name -> 1L,
                Long::sum
            ));
        return new MoodleGroupChangeBuilder(context, groupNames, groupingNames).buildMoodleGroupingChanges();
    }

    private List<MoodleGroupingChange> buildMoodleGroupingChanges() {
        List<MoodleGrouping> moodleGroupingsWithGroups = context.getMoodleGroupingsWithGroups();
        List<SisuStudyGroupSet> studyGroupSets = context.getSisuCourseUnitRealisation().studyGroupSets;

        // Handle existing Moodle groupings
        final List<MoodleGroupingChange> existingMoodleGroupingChanges = moodleGroupingsWithGroups
            .stream()
            .map(moodleGrouping -> {
                final Optional<SisuStudyGroupSet> studyGroupSet = studyGroupSets.stream()
                    .filter(s -> s.getLocalId().equals(moodleGrouping.getSisuStudyGroupSetLocalId()))
                    .findFirst();
                if (studyGroupSet.isPresent()) {
                    final SisuStudyGroupSet existingStudyGroupSet = studyGroupSet.get();
                    return MoodleGroupingChange.keepGrouping(
                        context.getMoodleCourse().id,
                        moodleGrouping.getId(),
                        existingStudyGroupSet.getLocalId(),
                        moodleGrouping.getIdNumber(),
                        moodleGrouping.getName(),
                        getChangedLocalizedText(
                            existingStudyGroupSet.getName(), moodleGrouping.getName(), context.getCourseLanguage(), groupingNames),
                        buildMoodleGroupChanges(moodleGrouping.getGroups(), existingStudyGroupSet.getStudySubGroups(), false)
                    );
                } else if (moodleGrouping.isSisuSynchronized()) {
                    return MoodleGroupingChange.deleteGrouping(
                        context.getMoodleCourse().id,
                        moodleGrouping.getIdNumber(),
                        moodleGrouping.getName(),
                        moodleGrouping.getId(),
                        buildMoodleGroupChanges(moodleGrouping.getGroups(), Collections.emptyList(), false)
                    );
                } else {
                    return MoodleGroupingChange.detachedGrouping(
                        context.getMoodleCourse().id,
                        moodleGrouping.getId(),
                        moodleGrouping.getIdNumber(),
                        moodleGrouping.getName(),
                        buildMoodleGroupChanges(moodleGrouping.getGroups(), Collections.emptyList(), true)
                    );
                }
            }).collect(java.util.stream.Collectors.toList());

        // Handle new groupings from Sisu
        final List<MoodleGroupingChange> newMoodleGroupingChanges = studyGroupSets.stream()
            .filter(MoodleGroupChangeBuilder::isValidStudyGroupSet)
            .filter(
                s -> moodleGroupingsWithGroups.stream()
                    .noneMatch(
                        g -> g.getSisuStudyGroupSetLocalId() != null
                            && g.getSisuStudyGroupSetLocalId().equals(s.getLocalId()))
            )
            .map(s -> MoodleGroupingChange.newGrouping(
                context.getMoodleCourse().id,
                s.getLocalId(),
                getChangedLocalizedText(s.getName(), null, context.getCourseLanguage(), groupingNames),
                buildMoodleGroupChanges(new ArrayList<>(), s.getStudySubGroups(), false)
        )).collect(Collectors.toList());
        existingMoodleGroupingChanges.addAll(newMoodleGroupingChanges);
        return existingMoodleGroupingChanges;
    }

    private @NotNull List<MoodleGroupChange> buildMoodleGroupChanges(
        List<MoodleGroup> moodleGroups,
        List<SisuStudySubGroup> sisuStudySubGroups,
        boolean isDetached
    ) {
        // Update existing Moodle groups
        final List<MoodleGroupChange> existingMoodleGroupChanges = moodleGroups.stream()
            .map(moodleGroup -> {
                final Optional<SisuStudySubGroup> studySubGroup = sisuStudySubGroups.stream()
                    .filter(sisuStudySubGroup -> sisuStudySubGroup.getId().equals(moodleGroup.getStudySubGroupId()))
                    .findFirst();
                if (studySubGroup.isPresent()) {
                    // Group is found from Sisu, so we keep the group.
                    final SisuStudySubGroup existingStudySubGroup = studySubGroup.get();
                    return MoodleGroupChange.keepGroup(
                        moodleGroup.getId(),
                        existingStudySubGroup.getId(),
                        moodleGroup.getIdNumber(),
                        moodleGroup.getName(),
                        getChangedLocalizedText(existingStudySubGroup.getName(), moodleGroup.getName(), context.getCourseLanguage(), groupNames),
                        buildMoodleGroupMembershipChanges(moodleGroup.getMembers(), existingStudySubGroup.getMemberIds(), false)
                    );
                } else if (moodleGroup.isSisuSynchronized() && !isDetached) {
                    // Previously synced group is not found from Sisu, so we delete the group.
                    return MoodleGroupChange.deleteGroup(moodleGroup.getId(), moodleGroup.getIdNumber(), moodleGroup.getName());
                } else {
                    // Group is not synced from Sisu so we don't touch it.
                    return MoodleGroupChange.detachedGroup(
                        moodleGroup.getId(),
                        moodleGroup.getIdNumber(),
                        moodleGroup.getName(),
                        buildMoodleGroupMembershipChanges(moodleGroup.getMembers(), Collections.emptyList(), true)
                    );
                }
            }).collect(java.util.stream.Collectors.toList());

        // Add new groups from Sisu
        List<MoodleGroupChange> newMoodleGroupChanges = new ArrayList<>();
        for (SisuStudySubGroup s : sisuStudySubGroups) {
            if (moodleGroups.stream().filter(g -> g.getStudySubGroupId() != null).noneMatch(g -> g.getStudySubGroupId().equals(s.getId()))) {
                newMoodleGroupChanges.add(
                    MoodleGroupChange.newGroup(
                        s.getId(),
                        getChangedLocalizedText(s.getName(), null, context.getCourseLanguage(), groupNames),
                        buildMoodleGroupMembershipChanges(new ArrayList<>(), s.getMemberIds(), false)
                    ));
            }
        }
        existingMoodleGroupChanges.addAll(newMoodleGroupChanges);
        return existingMoodleGroupChanges;
    }

    private List<MoodleCourseMembershipChange> buildMoodleGroupMembershipChanges(
        List<MoodleUser> members,
        @Nullable List<String> nullableSisuUserIds,
        boolean isDetachedGroup
    ) {
        List<String> sisuUserIds = Optional.ofNullable(nullableSisuUserIds).orElse(Collections.emptyList());
        List<MoodleCourseMembershipChange> changes = new ArrayList<>();
        Map<String, Long> moodleCourseMemberIdBySisuUserId = context.getMoodleCourseMembers().stream()
            .collect(java.util.stream.Collectors.toMap(MoodleUser::getUsername, MoodleUser::getId));
        for (MoodleUser member : members) {
            if (isDetachedGroup) {
                changes.add(MoodleCourseMembershipChange.detachedMembership(member.getId()));
                continue;
            }
            if (!sisuUserIds.contains(member.getUsername())) {
                changes.add(MoodleCourseMembershipChange.deleteMembership(member.getId()));
            } else {
                changes.add(MoodleCourseMembershipChange.keepMembership(member.getId(), member.getUsername()));
            }
        }
        for (String sisuUserId : sisuUserIds) {
            if (members.stream().noneMatch(m -> m.getUsername().equals(sisuUserId))) {
                Long moodleUserId = moodleCourseMemberIdBySisuUserId.get(sisuUserId);
                MoodleCourseMembershipChange membershipChange = MoodleCourseMembershipChange.newMembership(moodleUserId, sisuUserId);
                if (moodleUserId == null) {
                    membershipChange.addError("User not found from Moodle");
                    membershipChange.setStatus(MoodleChangeStatus.FAILED);
                }
                changes.add(membershipChange);
            }
        }
        return changes;
    }

    private static String getChangedLocalizedText(SisuLocalisedValue localisedValue,
                                                  String currentText,
                                                  String lang,
                                                  Map<String, Long> existingNames) {
        if (localisedValue.getForLocaleOrDefault(SisuLocale.byCodeOrDefaultToFi(lang)).equals(currentText)) {
            return null;
        }
        String changed = localisedValue.getForLocaleOrDefault(SisuLocale.byCodeOrDefaultToFi(lang));
        if (existingNames.containsKey(changed)) {
            changed = changed + " (" + existingNames.get(changed) + ")";
        }
        return changed;
    }
}
