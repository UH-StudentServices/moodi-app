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
import io.aexp.nodes.graphql.annotations.GraphQLIgnore;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.Constants.RESPONSIBLE_ORGANISATION;
import static fi.helsinki.moodi.Constants.TEACHER_TYPES;

@GraphQLProperty(name = "course_unit_realisation", arguments = {
    @GraphQLArgument(name = "id", type = "String")
})
public class SisuCourseUnitRealisation {

    @GraphQLIgnore
    static final Map<SisuLocale, String> COURSE_UNIT_LOCALIZATION = new HashMap<SisuLocale, String>() {
        {
            put(SisuLocale.FI, "Opintojaksot");
            put(SisuLocale.SV, "Studieavsnitten");
            put(SisuLocale.EN, "Courses");
        }
    };

    @GraphQLIgnore
    static final DateTimeFormatter FINNISH_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");

    public String id;
    public SisuLocalisedValue name;
    public SisuCourseUnitRealisationType courseUnitRealisationType;
    public SisuDateRange activityPeriod;
    public String flowState;
    public String teachingLanguageUrn;
    public List<SisuLearningEnvironment> learningEnvironments = new ArrayList<>();
    public List<SisuResponsibilityInfo> responsibilityInfos = new ArrayList<>();
    public List<SisuEnrolment> enrolments = new ArrayList<>();
    public List<SisuOrganisationRoleShare> organisations = new ArrayList<>();
    public List<SisuCourseUnit> courseUnits = new ArrayList<>();

    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation() {
        StudyRegistryCourseUnitRealisation ret = new StudyRegistryCourseUnitRealisation();
        SisuLocale teachingLanguageCode = SisuLocale.byUrnOrDefaultToFi(teachingLanguageUrn);
        ret.realisationId = id;
        ret.realisationName = name.getForLocaleOrDefault(teachingLanguageCode);
        ret.students = enrolments.stream().map(e -> e.person.toStudyRegistryStudent(e.isEnrolled())).collect(Collectors.toList());
        ret.published = "PUBLISHED".equals(flowState);

        if (activityPeriod != null) {
            ret.startDate = activityPeriod.startDate != null ? activityPeriod.startDate : LocalDate.now();
            ret.endDate = activityPeriod.endDate != null ? activityPeriod.endDate.plusMonths(1) : LocalDate.now().plusMonths(12);
        } else {
            ret.startDate = LocalDate.now();
            ret.endDate = LocalDate.now().plusMonths(12);
        }

        ret.mainOrganisationId = getMainOrganisationId();

        String defaultLocalizedUrl = getLearningEnvironmentUrl(learningEnvironments, SisuLocale.FI, "");

        String localizedUrls = getLocalizedSpan(SisuLocale.FI, getLearningEnvironmentUrl(learningEnvironments, SisuLocale.FI, defaultLocalizedUrl))
            + getLocalizedSpan(SisuLocale.EN, getLearningEnvironmentUrl(learningEnvironments, SisuLocale.EN, defaultLocalizedUrl))
            + getLocalizedSpan(SisuLocale.SV, getLearningEnvironmentUrl(learningEnvironments, SisuLocale.SV, defaultLocalizedUrl));

        String localizedCUNames = getLocalizedSpan(SisuLocale.FI, COURSE_UNIT_LOCALIZATION.get(SisuLocale.FI))
            + getLocalizedSpan(SisuLocale.EN, COURSE_UNIT_LOCALIZATION.get(SisuLocale.EN))
            + getLocalizedSpan(SisuLocale.SV, COURSE_UNIT_LOCALIZATION.get(SisuLocale.SV));

        String courseUnitCodes = courseUnits.stream()
            .sorted(Comparator.comparing(cu -> cu.code))
            .map(cu -> cu.code)
            .collect(Collectors.joining(", "));

        String localizedCUTypes = getLocalizedSpan(SisuLocale.FI, courseUnitRealisationType.name.getForLocaleOrDefault(SisuLocale.FI))
            + getLocalizedSpan(SisuLocale.EN, courseUnitRealisationType.name.getForLocaleOrDefault(SisuLocale.EN))
            + getLocalizedSpan(SisuLocale.SV, courseUnitRealisationType.name.getForLocaleOrDefault(SisuLocale.SV));

        ret.description = "<p>" + localizedUrls + "</p>"
            + "<p>" + localizedCUNames + ", " + courseUnitCodes + "</p>"
            + "<p>" + localizedCUTypes + ", " + FINNISH_DATE_FORMAT.format(ret.startDate) + "-" + FINNISH_DATE_FORMAT.format(ret.endDate) + "</p>";

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

    private String getLearningEnvironmentUrl(List<SisuLearningEnvironment> learningEnvironments, SisuLocale sisuLocale, String defaultUrl) {
        SisuLearningEnvironment le = learningEnvironments.stream()
            .filter(x -> SisuLocale.byCodeOrDefaultToFi(x.language).equals(sisuLocale))
            .findFirst().orElse(null);
        if (le == null) {
            return defaultUrl;
        }
        return le.url == null ? defaultUrl : "<a href=\"" + le.url + "\">" + le.url + "</a>";
    }

    private String getLocalizedSpan(SisuLocale locale, String text) {
        String languageSpanStart = "<span lang=\"%lang%\" class=\"multilang\">";
        String languageSpanEnd = "</span>";
        return languageSpanStart.replace("%lang%", locale.toString().toLowerCase()) + text + languageSpanEnd;
    }

    private String getMainOrganisationId() {
        return organisations.stream()
            .filter(o -> RESPONSIBLE_ORGANISATION.equals(o.roleUrn) && o.share > 0.5)
            .findFirst()
            .map(o -> o.organisation.id)
            .orElse(null);
    }

    public List<String> teacherSisuIds() {
        return responsibilityInfos.stream()
            .filter(r -> TEACHER_TYPES.contains(r.roleUrn))
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
