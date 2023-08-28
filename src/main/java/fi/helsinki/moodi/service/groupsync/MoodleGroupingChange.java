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
import java.util.List;

@Getter
public class MoodleGroupingChange extends MoodleChange {
    @NotNull private final Long moodleCourseId;
    @Nullable private final String sisuStudySubGroupSetLocalId;
    @Nullable private final Long moodleGroupingId;
    @Nullable private final String moodleIdNumber;
    @Nullable private final String currentName;
    @Nullable private final String newName;
    @NotNull
    private List<MoodleGroupChange> groups;

    private MoodleGroupingChange(
        @NotNull Long moodleCourseId,
        @Nullable String sisuStudySubGroupSetLocalId,
        @Nullable Long moodleGroupingId,
        @Nullable String moodleIdNumber,
        @Nullable String currentName,
        @Nullable String newName,
        List<MoodleGroupChange> groups,
        MoodleChangeType changeType) {
        super(changeType);
        this.moodleCourseId = moodleCourseId;
        this.moodleIdNumber = moodleIdNumber;
        this.sisuStudySubGroupSetLocalId = sisuStudySubGroupSetLocalId;
        this.moodleGroupingId = moodleGroupingId;
        this.currentName = currentName;
        this.newName = newName;
        this.groups = groups;
    }

    public static MoodleGroupingChange newGrouping(
        @NotNull Long moodleCourseId,
        @NotNull String sisuStudySubGroupSetLocalId,
        @NotNull String newName,
        @NotNull List<MoodleGroupChange> groupChanges) {
        return new MoodleGroupingChange(
            moodleCourseId,
            sisuStudySubGroupSetLocalId,
            null,
            Constants.MOODLE_SISU_ID_PREFIX + sisuStudySubGroupSetLocalId,
            null,
            newName,
            groupChanges,
            MoodleChangeType.CREATE
        );
    }

    public static MoodleGroupingChange deleteGrouping(
        @NotNull Long moodleCourseId,
        String moodleIdNumber,
        String currentName,
        @NotNull Long moodleGroupingId,
        @NotNull List<MoodleGroupChange> groupChanges
    ) {
        return new MoodleGroupingChange(
            moodleCourseId,
            null,
            moodleGroupingId,
            moodleIdNumber,
            currentName,
            null,
            groupChanges,
            MoodleChangeType.DELETE
        );
    }

    public static MoodleGroupingChange keepGrouping(
        @NotNull Long moodleCourseId,
        @NotNull Long moodleGroupingId,
        @NotNull String sisuStudySubGroupSetLocalId,
        String moodleIdNumber,
        String currentName,
        String newName,
        @NotNull List<MoodleGroupChange> groupChanges) {
        boolean hasChanges = newName != null;
        return new MoodleGroupingChange(
            moodleCourseId,
            sisuStudySubGroupSetLocalId,
            moodleGroupingId,
            moodleIdNumber,
            currentName,
            newName,
            groupChanges,
            hasChanges ? MoodleChangeType.UPDATE : MoodleChangeType.KEEP);
    }

    public static MoodleGroupingChange detachedGrouping(@NotNull Long moodleCourseId,
                                                        @NotNull Long moodleGroupingId,
                                                        String moodleIdNumber,
                                                        String currentName,
                                                        List<MoodleGroupChange> moodleGroupChanges) {
        return new MoodleGroupingChange(
            moodleCourseId,
            null,
            moodleGroupingId,
            moodleIdNumber,
            currentName,
            null,
            moodleGroupChanges,
            MoodleChangeType.DETACHED
        );
    }
}
