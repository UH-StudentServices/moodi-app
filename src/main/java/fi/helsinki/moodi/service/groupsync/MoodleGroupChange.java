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

import fi.helsinki.moodi.Constants;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Getter
public class MoodleGroupChange extends MoodleChange {
    @Nullable
    private final Long moodleGroupId;
    @Nullable
    private final String sisuStudySubGroupId;
    @Nullable String moodleIdNumber;
    @Nullable
    private final String moodleCurrentName;
    @Nullable
    private final String newNameFromSisu;
    @NotNull
    private List<MoodleCourseMembershipChange> memberships;

    private MoodleGroupChange(
        @Nullable Long moodleGroupId,
        @Nullable String sisuStudySubGroupId,
        @Nullable String moodleIdNumber,
        @Nullable String moodleCurrentName,
        @Nullable String newNameFromSisu,
        @NotNull MoodleChangeType changeType,
        @NotNull List<MoodleCourseMembershipChange> memberships) {
        super(changeType);
        this.moodleGroupId = moodleGroupId;
        this.sisuStudySubGroupId = sisuStudySubGroupId;
        this.moodleIdNumber = moodleIdNumber;
        this.moodleCurrentName = moodleCurrentName;
        this.newNameFromSisu = newNameFromSisu;
        this.memberships = memberships;
    }

    public static MoodleGroupChange newGroup(
        String studySubGroupId,
        String newName,
        List<MoodleCourseMembershipChange> membershipChanges) {
        return new MoodleGroupChange(
            null,
            studySubGroupId,
            Constants.MOODLE_SISU_ID_PREFIX + studySubGroupId,
            null,
            newName,
            MoodleChangeType.CREATE,
            membershipChanges
        );
    }

    public static MoodleGroupChange deleteGroup(Long moodleGroupId, String moodleIdNumber, String currentName) {
        return new MoodleGroupChange(
            moodleGroupId,
            null,
            moodleIdNumber,
            currentName,
            null,
            MoodleChangeType.DELETE,
            Collections.emptyList()
        );
    }

    public static MoodleGroupChange keepGroup(
        @NotNull Long moodleGroupId,
        @NotNull String sisuGroupId,
        @Nullable String moodleIdNumber,
        @Nullable String currentName,
        @Nullable String newName,
        List<MoodleCourseMembershipChange> membershipChanges) {
        boolean hasChanges = newName != null;
        return new MoodleGroupChange(
            moodleGroupId,
            sisuGroupId,
            moodleIdNumber,
            currentName,
            newName,
            hasChanges ? MoodleChangeType.UPDATE : MoodleChangeType.KEEP,
            membershipChanges);
    }

    public static MoodleGroupChange detachedGroup(
        @NotNull Long moodleGroupId,
        @Nullable String moodleIdNumber,
        @Nullable String currentName,
        List<MoodleCourseMembershipChange> membershipChanges
    ) {
        return new MoodleGroupChange(
            moodleGroupId,
            null,
            moodleIdNumber,
            currentName,
            null,
            MoodleChangeType.DETACHED,
            membershipChanges
        );
    }

}

