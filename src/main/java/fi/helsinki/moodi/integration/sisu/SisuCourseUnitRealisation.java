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
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.Constants.RESPONSIBLE_ORGANISATION;
import static fi.helsinki.moodi.Constants.TEACHER_TYPES;

@GraphQLProperty(name = "course_unit_realisation", arguments = {
    @GraphQLArgument(name = "id", type = "String")
})
public class SisuCourseUnitRealisation {
    @GraphQLIgnore
    public static final int MAX_NAME_DB_LENGTH = 255;
    @GraphQLIgnore
    public static final int SPAN_SIZE = 41;

    @GraphQLIgnore
    private static final Logger LOGGER = LoggerFactory.getLogger(SisuCourseUnitRealisation.class);
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
    @GraphQLIgnore
    public static final String SEPARATOR = ", ";

    public String id;
    public SisuLocalisedValue name;
    public SisuLocalisedValue nameSpecifier;
    public SisuCourseUnitRealisationType courseUnitRealisationType;
    public SisuDateRange activityPeriod;
    public String flowState;
    public String teachingLanguageUrn;
    public List<SisuLearningEnvironment> learningEnvironments = new ArrayList<>();
    public List<SisuResponsibilityInfo> responsibilityInfos = new ArrayList<>();
    public List<SisuStudyGroupSet> studyGroupSets = new ArrayList<>();
    public List<SisuEnrolment> enrolments = new ArrayList<>();
    public List<SisuOrganisationRoleShare> organisations = new ArrayList<>();
    public List<SisuCourseUnit> courseUnits = new ArrayList<>();

    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation() {
        LOGGER.info("Generating StudyRegistryCourseUnitRealisation for {}", id);
        StudyRegistryCourseUnitRealisation ret = new StudyRegistryCourseUnitRealisation();
        SisuLocale teachingLanguageCode = SisuLocale.byUrnOrDefaultToFi(teachingLanguageUrn);
        ret.realisationId = id;
        ret.realisationName = generateName(teachingLanguageCode);
        ret.teachingLanguageRealisationName = calculateName(teachingLanguageCode);
        ret.students = enrolments.stream()
            .filter(e -> e.person != null)
            .map(e -> e.person.toStudyRegistryStudent(e.isEnrolled()))
            .collect(Collectors.toList());
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

        String courseDatesPart = "";
        if (activityPeriod != null && activityPeriod.startDate != null) {
            String endPart = "";
            // endDates are exclusive, so startDate 14.5. and endDate 15.5 would mean that the event is single day 14.5.
            // so remove one day from all end dates and don't display them as ranges if there is only one day.
            if (activityPeriod.endDate == null) {
                endPart = "–";
            } else if (activityPeriod.endDate.minusDays(1).isAfter(activityPeriod.startDate)) {
                endPart = "–" + FINNISH_DATE_FORMAT.format(activityPeriod.endDate.minusDays(1));
            }
            courseDatesPart = ", " + FINNISH_DATE_FORMAT.format(activityPeriod.startDate) + endPart;
        }

        ret.description = "<p>" + localizedUrls + "</p>"
            + "<p>" + localizedCUNames + " " + courseUnitCodes + "</p>"
            + "<p>" + localizedCUTypes + courseDatesPart + "</p>";

        return ret;
    }

    // Sisu CUR data does not contain teacher user name nor employee number, so teachers need to be populated separately.
    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation(Map<String, StudyRegistryTeacher> teachersById) {
        StudyRegistryCourseUnitRealisation ret = this.toStudyRegistryCourseUnitRealisation();

        this.teacherSisuIds().forEach(id -> {
            if (teachersById.get(id) != null) {
                ret.teachers.add(teachersById.get(id));
            }
        });

        return ret;
    }

    protected String generateName(SisuLocale teachingLanguage) {
        SisuLocale otherLang1 = getOtherLanguage(teachingLanguage);
        SisuLocale otherLang2 = getOtherLanguage(teachingLanguage, otherLang1);

        String defaultName = calculateName(teachingLanguage);
        String otherName1 = calculateName(otherLang1);
        String otherName2 = calculateName(otherLang2);

        String compDefault = defaultName != null ? defaultName.trim() : "";
        String comp1 = otherName1 != null ? otherName1.trim() : "";
        String comp2 = otherName2 != null ? otherName2.trim() : "";

        String defaultFinal = "";
        String other1Final = "";
        String other2Final = "";

        boolean allNamesAreSame = compDefault.equalsIgnoreCase(comp1) && compDefault.equalsIgnoreCase(comp2);

        // default name = teaching language name
        // 1 cases when there is no localisation:
        // 1.1 if all names are same
        //      -> return the default name
        // 1.2 if other names are empty or null
        //      -> return the default name
        // 1.3 if default name is empty but one other name exists
        //      -> return existing name
        // 1.4 the total number of characters in localised languages is higher than db max_length - (number of languages * localisation span size)
        //     -> return default name
        // 2 cases when there is localisation:
        // 2.1 if at least 2 languages are not null and are not equal
        //     -> return localised non-null non-empty languages that are not equal to default language
        // 2.2 see 1.4. (total length fits within the db max_length)

        // 1.1., 1.2 and 1.3
        if (allNamesAreSame) {
            return defaultName;
        } else if (StringUtils.isNotEmpty(compDefault) && StringUtils.isEmpty(comp1) && StringUtils.isEmpty(comp2)) {
            return defaultName;
        } else if (StringUtils.isEmpty(compDefault) && StringUtils.isNotEmpty(comp1) && StringUtils.isEmpty(comp2)) {
            return otherName1;
        } else if (StringUtils.isEmpty(compDefault) && StringUtils.isEmpty(comp1) && StringUtils.isNotEmpty(comp2)) {
            return otherName2;
        }

        if (StringUtils.isNotEmpty(compDefault)) {
            defaultFinal = getLocalizedSpan(teachingLanguage, defaultName);
        }
        // determine comp1 and comp2
        if (StringUtils.isNotEmpty(comp1) && !compDefault.equals(comp1)) {
            other1Final = getLocalizedSpan(otherLang1, otherName1);
        }
        if (StringUtils.isNotEmpty(comp2) && !compDefault.equals(comp2)) {
            other2Final = getLocalizedSpan(otherLang2, otherName2);
        }

        // if total length exceeds maximum length revert to non-localized teaching language name
        if (defaultFinal.length() + other1Final.length() + other2Final.length() >= MAX_NAME_DB_LENGTH) {
            if (StringUtils.isNotEmpty(compDefault)) {
                return defaultName;
            } else if (StringUtils.isNotEmpty(comp1)) {
                return otherName1;
            } else if (StringUtils.isNotEmpty(comp2)) {
                return otherName2;
            }
            return "";
        }

        return defaultFinal + other1Final + other2Final;
    }

    protected String calculateName(SisuLocale locale) {
        String n = name != null ? name.getForLocale(locale) : null;
        String ns = nameSpecifier != null ? nameSpecifier.getForLocale(locale) : null;
        if (id.toLowerCase().startsWith("hy-opt-cur")) {
            return n;
        } else if (id.toLowerCase().startsWith("hy-cur") && !id.toLowerCase().contains("aili")) {
            return combineFields(n, ns);
        } else {
            return combineFields(ns, n);
        }
    }

    private String combineFields(String value, String value2) {
        if (StringUtils.isBlank(value) && StringUtils.isBlank(value2)) {
            return null;
        }
        if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(value2)) {
            return value + SEPARATOR + value2;
        }
        if (StringUtils.isBlank(value2)) {
            return value;
        }
        return value2;
    }

    private SisuLocale getOtherLanguage(SisuLocale... languages) {
        for (SisuLocale locale : SisuLocale.values()) {
            if (!Arrays.asList(languages).contains(locale)) {
                return locale;
            }
        }
        return null;
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
        if (organisations == null || organisations.isEmpty()) {
            return null;
        }
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
