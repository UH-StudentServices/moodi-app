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

package fi.helsinki.moodi.integration.sisu;

import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLProperty(name = "course_unit_realisation", arguments = {
    @GraphQLArgument(name = "id", type = "String")
})
public class SisuCourseUnitRealisation {

    public String id;
    public SisuLocalisedValue name;
    public SisuDateRange activityPeriod;
    public String flowState;
    public String teachingLanguageUrn;
    public List<SisuLearningEnvironment> learningEnvironments = new ArrayList<>();
    public List<SisuResponsibilityInfo> responsibilityInfos = new ArrayList<>();
    public List<SisuEnrolment> enrolments = new ArrayList<>();

    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation() {
        StudyRegistryCourseUnitRealisation ret = new StudyRegistryCourseUnitRealisation();
        SisuLocale teachingLanguageCode = SisuLocale.byUrnOrDefaultToFi(teachingLanguageUrn);
        ret.origin = StudyRegistryCourseUnitRealisation.Origin.SISU;
        ret.realisationId = id;
        ret.realisationName = name.getForLocaleOrDefault(teachingLanguageCode);
        ret.students = enrolments.stream().map(e -> e.person.toStudyRegistryStudent(e.isEnrolled())).collect(Collectors.toList());
        // Sisu CUR data does not contain teacher user name nor employee number, so we leave this null here,
        // and it will be populated in StudyRegistryService.
        ret.teachers = null;
        ret.published = "PUBLISHED".equals(flowState);

        if (activityPeriod != null) {
            ret.startDate = activityPeriod.startDate != null ? activityPeriod.startDate : LocalDate.now();
            ret.endDate =  activityPeriod.endDate != null ? activityPeriod.endDate.plusMonths(1) : LocalDate.now().plusMonths(12);
        } else {
            ret.startDate = LocalDate.now();
            ret.endDate = LocalDate.now().plusMonths(12);
        }

        // Use the URL of primary learningEnvironment, in language of the course teaching language, or if
        // that language is not found, some other language.
        ret.description = learningEnvironments.stream()
            .filter(le -> le.primary)
            .sorted((le1, le2) -> SisuLocale.byCodeOrDefaultToFi(le1.language).equals(teachingLanguageCode) ? -1 : 1)
            .map(le -> le.url)
            .findFirst().orElse("");

        return ret;
    }

    public List<String> teacherSisuIds() {
        return responsibilityInfos.stream()
            .filter(r -> r.roleUrn != null && r.roleUrn.contains("teacher"))
            .map(r -> r.personId).collect(Collectors.toList());
    }
}
