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

import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

import java.util.ArrayList;
import java.util.List;

@GraphQLProperty(name = "course_unit_realisation", arguments = {
    @GraphQLArgument(name = "id", type = "String")
})
public class SisuCourseUnitRealisation {
    public enum DocumentState {
        DRAFT,
        ACTIVE,
        DELETED
    }

    public enum FlowState {
        NOT_READY,
        PUBLISHED,
        CANCELLED,
        ARCHIVED
    }

    public String id;
    public SisuLocalisedValue name;
    public SisuLocalisedValue nameSpecifier;
    public SisuCourseUnitRealisationType courseUnitRealisationType;
    public SisuDateRange activityPeriod;
    public DocumentState documentState;
    public FlowState flowState;
    public String teachingLanguageUrn;
    public List<SisuLearningEnvironment> learningEnvironments = new ArrayList<>();
    public List<SisuResponsibilityInfo> responsibilityInfos = new ArrayList<>();
    public List<SisuStudyGroupSet> studyGroupSets = new ArrayList<>();
    public List<SisuEnrolment> enrolments = new ArrayList<>();
    public List<SisuOrganisationRoleShare> organisations = new ArrayList<>();
    public List<SisuCourseUnit> courseUnits = new ArrayList<>();

    public static class SisuCURWrapper {
        @GraphQLProperty(name = "course_unit_realisations", arguments = {
            @GraphQLArgument(name = "ids", type = "String")
        })
        @SuppressWarnings("checkstyle:MemberName")
        public List<SisuCourseUnitRealisation> course_unit_realisations;
    }
}
