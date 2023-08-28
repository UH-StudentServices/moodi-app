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
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MoodleGroup {
    private Long id;

    private Long courseId;
    private String name;
    private String description;
    private Long descriptionFormat;
    private String enrolmentKey;
    private String idNumber;
    private String studySubGroupId;
    private boolean isSisuSynchronized;
    @Setter List<MoodleUser> members;

    public static MoodleGroup fromData(MoodleGroupData data) {
        boolean isSisuSynchronized = data.getIdNumber().startsWith(Constants.MOODLE_SISU_ID_PREFIX);

        return new MoodleGroup(
            data.getId(),
            data.getCourseId(),
            data.getName(),
            data.getDescription(),
            data.getDescriptionFormat(),
            data.getEnrolmentKey(),
            data.getIdNumber(),
            isSisuSynchronized ? data.getIdNumber().replaceFirst(Constants.MOODLE_SISU_ID_PREFIX, "") : null,
            isSisuSynchronized,
            data.getMembers()
        );
    }

    /**
     * Creates a new group to be added to Moodle. Memberships are assigned later.
     *
     * @param courseId Moodle course id, to which the grouping is added.
     * @param name Group name.
     * @param description Group description.
     * @param enrolmentKey Group enrolment key.
     * @param sisuStudySubGroupId Sisu id of the grouping. sisu: prefix is added automatically, which is used to identify Sisu synced groups.
     * @return New Moodle group object.
     */
    public static MoodleGroup newGroupFromSisu(Long courseId, String name, String description, String enrolmentKey, String sisuStudySubGroupId) {
        return new MoodleGroup(
            null,
            courseId,
            name,
            description,
            Constants.MOODLE_DESCRIPTION_FORMAT_HTML,
            enrolmentKey,
            Constants.MOODLE_SISU_ID_PREFIX + sisuStudySubGroupId,
            sisuStudySubGroupId,
            true,
            Collections.emptyList()
        );
    }
}
