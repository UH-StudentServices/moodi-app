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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.integration.sisu.SisuOrganisationRoleShare.RESPONSIBLE;

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
    public List<SisuOrganisationRoleShare> organisations = new ArrayList<>();

    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation() {
        StudyRegistryCourseUnitRealisation ret = new StudyRegistryCourseUnitRealisation();
        SisuLocale teachingLanguageCode = SisuLocale.byUrnOrDefaultToFi(teachingLanguageUrn);
        ret.origin = StudyRegistryCourseUnitRealisation.Origin.SISU;
        ret.realisationId = id;
        ret.realisationName = name.getForLocaleOrDefault(teachingLanguageCode);
        ret.students = enrolments.stream().map(e -> e.person.toStudyRegistryStudent(e.isEnrolled())).collect(Collectors.toList());
        ret.published = "PUBLISHED".equals(flowState);

        if (activityPeriod != null) {
            ret.startDate = activityPeriod.startDate != null ? activityPeriod.startDate : LocalDate.now();
            ret.endDate =  activityPeriod.endDate != null ? activityPeriod.endDate.plusMonths(1) : LocalDate.now().plusMonths(12);
        } else {
            ret.startDate = LocalDate.now();
            ret.endDate = LocalDate.now().plusMonths(12);
        }

        ret.mainOrganisationId = getMainOrganisationId();

        // Use the URL of primary learningEnvironment, in language of the course teaching language, or if
        // that language is not found, some other language.
        ret.description = learningEnvironments.stream()
            .filter(le -> le.primary)
            .sorted((le1, le2) -> SisuLocale.byCodeOrDefaultToFi(le1.language).equals(teachingLanguageCode) ? -1 : 1)
            .map(le -> le.url)
            .findFirst().orElse("");

        return ret;
    }

    // Sisu CUR data does not contain teacher user name nor employee number, so teachers need to be populated separately.
    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation(Map<String, StudyRegistryTeacher> teachersById) {
        StudyRegistryCourseUnitRealisation ret = this.toStudyRegistryCourseUnitRealisation();

        this.teacherSisuIds().stream().forEach(id -> {
            if (teachersById.get(id) != null) {
                ret.teachers.add(teachersById.get(id));
            }
        });

        return ret;
    }

    private String getMainOrganisationId() {
        return organisations.stream()
                .filter(o -> RESPONSIBLE.equals(o.roleUrn) && o.share > 0.5)
                .findFirst()
                .map(o -> o.organisation.id)
                .orElse(null);
    }

    public List<String> teacherSisuIds() {
        return responsibilityInfos.stream()
            // Accept both responsible teacher and teacher.
            .filter(r -> r.roleUrn != null && r.roleUrn.contains("teacher"))
            .map(r -> r.personId).collect(Collectors.toList());
    }

    public static class SisuCURWrapper {
        @GraphQLProperty(name = "course_unit_realisations", arguments = {
            @GraphQLArgument(name = "ids", type = "String")
        })
        @SuppressWarnings("checkstyle:MemberName")
        public List<SisuCourseUnitRealisation> course_unit_realisations;
    }
}
