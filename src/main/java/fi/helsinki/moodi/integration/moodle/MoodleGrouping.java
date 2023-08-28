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

package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.Constants;
import fi.helsinki.moodi.integration.sisu.SisuLocale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MoodleGrouping {
    @Setter private Long id;
    private Long courseId;

    private String name;
    private String description;
    private Long descriptionFormat;
    private String idNumber;
    private String sisuStudyGroupSetLocalId;
    private boolean isSisuSynchronized;
    private List<MoodleGroup> groups;

    public static MoodleGrouping fromData(MoodleGroupingData data) {
        boolean isSisuSynchronized = data.getIdNumber().startsWith(Constants.MOODLE_SISU_ID_PREFIX);

        return new MoodleGrouping(
            data.getId(),
            data.getCourseId(),
            data.getName(),
            data.getDescription(),
            data.getDescriptionFormat(),
            data.getIdNumber(),
            isSisuSynchronized ? data.getIdNumber().replaceFirst(Constants.MOODLE_SISU_ID_PREFIX, "") : null,
            isSisuSynchronized,
            data.getGroups().stream().map(MoodleGroup::fromData).collect(Collectors.toList())
        );
    }

    /**
     * Creates a new grouping to be added to Moodle. Groups are assigned to the grouping later.
     *
     * @param courseId Moodle course id, to which the grouping is added.
     * @param name Name of the grouping.
     * @param sisuId Sisu id of the grouping. sisu: prefix is added automatically, which is used to identify Sisu groupings.
     * @return New Moodle grouping object.
     */
    public static MoodleGrouping newMoodleGrouping(Long courseId, String name, String sisuId) {
        return new MoodleGrouping(null,
            courseId,
            name,
            null,
            Constants.MOODLE_DESCRIPTION_FORMAT_HTML,
            Constants.MOODLE_SISU_ID_PREFIX + sisuId,
            sisuId,
            true,
            Collections.emptyList()
        );
    }

    public static MoodleGrouping newSisuCommonGrouping(Long courseId, String langCode) {
        return new MoodleGrouping(null,
            courseId,
            Constants.SISU_COMMON_GROUPING_NAME.getForLocaleOrDefault(SisuLocale.valueOf(langCode.toUpperCase())),
            // TODO: add static description
            null,
            Constants.MOODLE_DESCRIPTION_FORMAT_HTML,
            Constants.MOODLE_SISU_COMMON_GROUPING_ID,
            null,
            false,
            Collections.emptyList()
        );
    }
}
