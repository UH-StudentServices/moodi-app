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

package fi.helsinki.moodi.service.course.group;

import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuLocalisedValue;

import java.util.*;

public final class SynchronizeGroupsResponse {
    public static class StudySubGroup {
        public String id;
        public SisuLocalisedValue name;
        public List<String> personIds;
        public SisuLocalisedValue studyGroupSetName;
        public String studyGroupSetId;
    }

    public List<StudySubGroup> studySubGroups;

    public SynchronizeGroupsResponse(SisuCourseUnitRealisation realisation) {
        Map<String, Set<String>> personIdsByStudySubGroupId = new HashMap<>();
        studySubGroups = new ArrayList<>();

        realisation.enrolments.forEach(enrolment -> {
            enrolment.confirmedStudySubGroupIds.forEach(subGroup -> {
                if (!personIdsByStudySubGroupId.containsKey(subGroup)) {
                    personIdsByStudySubGroupId.put(subGroup, new HashSet<>());
                }
                personIdsByStudySubGroupId.get(subGroup).add(enrolment.person.id);
            });
        });
        realisation.studyGroupSets.forEach(set -> {
            set.studySubGroups.forEach(subGroup -> {
                StudySubGroup studySubGroup = new StudySubGroup();
                studySubGroup.id = subGroup.id;
                studySubGroup.name = subGroup.name;
                studySubGroup.studyGroupSetName = set.name;
                studySubGroup.studyGroupSetId = set.localId;
                studySubGroup.personIds = new ArrayList<>(personIdsByStudySubGroupId.getOrDefault(subGroup.id, new HashSet<>()));
                studySubGroups.add(studySubGroup);
            });
        });

    }
}
