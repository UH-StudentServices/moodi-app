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
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.Constants.RESPONSIBLE_ORGANISATION;
import static fi.helsinki.moodi.Constants.TEACHER_TYPES;

public final class SisuCourseUnitRealisationConverter {

    public static final int MAX_NAME_DB_LENGTH = 255;
    public static final int SPAN_SIZE = 41;
    public static final String SEPARATOR = ", ";

    static final Map<SisuLocale, String> COURSE_UNIT_LOCALIZATION = new EnumMap<>(SisuLocale.class) {
        {
            put(SisuLocale.FI, "Opintojaksot");
            put(SisuLocale.SV, "Studieavsnitten");
            put(SisuLocale.EN, "Courses");
        }
    };

    static final DateTimeFormatter FINNISH_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");

    private static final Logger LOGGER = LoggerFactory.getLogger(SisuCourseUnitRealisationConverter.class);

    private SisuCourseUnitRealisationConverter() {
    }

    public static StudyRegistryCourseUnitRealisation convert(SisuCourseUnitRealisation cur) {
        LOGGER.info("Generating StudyRegistryCourseUnitRealisation for {}", cur.id);
        StudyRegistryCourseUnitRealisation ret = new StudyRegistryCourseUnitRealisation();
        SisuLocale teachingLanguageCode = SisuLocale.byUrnOrDefaultToFi(cur.teachingLanguageUrn);
        ret.realisationId = cur.id;
        ret.realisationName = generateName(cur, teachingLanguageCode);
        ret.teachingLanguageRealisationName = Objects.requireNonNullElse(calculateName(cur, teachingLanguageCode), ret.realisationName);
        ret.students = cur.enrolments.stream()
            .filter(e -> e.person != null)
            .map(e -> e.person.toStudyRegistryStudent(e.isEnrolled()))
            .toList();
        ret.flowState = cur.flowState;
        ret.documentState = cur.documentState;

        if (cur.activityPeriod != null) {
            ret.startDate = cur.activityPeriod.startDate != null ? cur.activityPeriod.startDate : LocalDate.now();
            ret.endDate = cur.activityPeriod.endDate != null ? cur.activityPeriod.endDate.plusMonths(1) : LocalDate.now().plusMonths(12);
        } else {
            ret.startDate = LocalDate.now();
            ret.endDate = LocalDate.now().plusMonths(12);
        }

        ret.mainOrganisationId = getMainOrganisationId(cur);

        String defaultLocalizedUrl = getLearningEnvironmentUrl(cur.learningEnvironments, SisuLocale.FI, "");

        String localizedUrls =
            getLocalizedSpan(SisuLocale.FI, getLearningEnvironmentUrl(cur.learningEnvironments, SisuLocale.FI, defaultLocalizedUrl))
            + getLocalizedSpan(SisuLocale.EN, getLearningEnvironmentUrl(cur.learningEnvironments, SisuLocale.EN, defaultLocalizedUrl))
            + getLocalizedSpan(SisuLocale.SV, getLearningEnvironmentUrl(cur.learningEnvironments, SisuLocale.SV, defaultLocalizedUrl));

        String localizedCUNames = getLocalizedSpan(SisuLocale.FI, COURSE_UNIT_LOCALIZATION.get(SisuLocale.FI))
            + getLocalizedSpan(SisuLocale.EN, COURSE_UNIT_LOCALIZATION.get(SisuLocale.EN))
            + getLocalizedSpan(SisuLocale.SV, COURSE_UNIT_LOCALIZATION.get(SisuLocale.SV));

        String courseUnitCodes = cur.courseUnits.stream()
            .sorted(Comparator.comparing(cu -> cu.code))
            .map(cu -> cu.code)
            .collect(Collectors.joining(", "));

        String localizedCUTypes = getLocalizedSpan(SisuLocale.FI, cur.courseUnitRealisationType.name.getForLocaleOrDefault(SisuLocale.FI))
            + getLocalizedSpan(SisuLocale.EN, cur.courseUnitRealisationType.name.getForLocaleOrDefault(SisuLocale.EN))
            + getLocalizedSpan(SisuLocale.SV, cur.courseUnitRealisationType.name.getForLocaleOrDefault(SisuLocale.SV));

        String courseDatesPart = "";
        if (cur.activityPeriod != null && cur.activityPeriod.startDate != null) {
            String endPart = "";
            // endDates are exclusive, so startDate 14.5. and endDate 15.5 would mean that the event is single day 14.5.
            // so remove one day from all end dates and don't display them as ranges if there is only one day.
            if (cur.activityPeriod.endDate == null) {
                endPart = "–";
            } else if (cur.activityPeriod.endDate.minusDays(1).isAfter(cur.activityPeriod.startDate)) {
                endPart = "–" + FINNISH_DATE_FORMAT.format(cur.activityPeriod.endDate.minusDays(1));
            }
            courseDatesPart = ", " + FINNISH_DATE_FORMAT.format(cur.activityPeriod.startDate) + endPart;
        }

        ret.description = "<p>" + localizedUrls + "</p>"
            + "<p>" + localizedCUNames + " " + courseUnitCodes + "</p>"
            + "<p>" + localizedCUTypes + courseDatesPart + "</p>";

        return ret;
    }

    public static StudyRegistryCourseUnitRealisation convert(SisuCourseUnitRealisation cur, Map<String, StudyRegistryTeacher> teachersById) {
        StudyRegistryCourseUnitRealisation ret = convert(cur);

        teacherSisuIds(cur).forEach(id -> {
            if (teachersById.get(id) != null) {
                ret.teachers.add(teachersById.get(id));
            }
        });

        return ret;
    }

    public static String generateName(SisuCourseUnitRealisation cur, SisuLocale teachingLanguage) {
        SisuLocale otherLang1 = getOtherLanguage(teachingLanguage);
        SisuLocale otherLang2 = getOtherLanguage(teachingLanguage, otherLang1);

        String defaultName = calculateName(cur, teachingLanguage);
        String otherName1 = calculateName(cur, otherLang1);
        String otherName2 = calculateName(cur, otherLang2);

        String compDefault = defaultName != null ? defaultName.trim() : "";
        String comp1 = otherName1 != null ? otherName1.trim() : "";
        String comp2 = otherName2 != null ? otherName2.trim() : "";

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

        String defaultFinal = "";
        String other1Final = "";
        String other2Final = "";
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

    public static String calculateName(SisuCourseUnitRealisation cur, SisuLocale locale) {
        String n = cur.name != null ? cur.name.getForLocale(locale) : null;
        String ns = cur.nameSpecifier != null ? cur.nameSpecifier.getForLocale(locale) : null;
        if (cur.id.toLowerCase().startsWith("hy-opt-cur")) {
            return n;
        } else if (cur.id.toLowerCase().startsWith("hy-cur") && !cur.id.toLowerCase().contains("aili")) {
            return combineFields(n, ns);
        } else {
            return combineFields(ns, n);
        }
    }

    public static List<String> teacherSisuIds(SisuCourseUnitRealisation cur) {
        return cur.responsibilityInfos.stream()
            .filter(r -> TEACHER_TYPES.contains(r.roleUrn))
            .map(r -> r.personId).toList();
    }

    private static String combineFields(String value, String value2) {
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

    private static SisuLocale getOtherLanguage(SisuLocale... languages) {
        for (SisuLocale locale : SisuLocale.values()) {
            if (!Arrays.asList(languages).contains(locale)) {
                return locale;
            }
        }
        return null;
    }

    private static String getLearningEnvironmentUrl(List<SisuLearningEnvironment> learningEnvironments, SisuLocale sisuLocale, String defaultUrl) {
        SisuLearningEnvironment le = learningEnvironments.stream()
            .filter(x -> SisuLocale.byCodeOrDefaultToFi(x.language).equals(sisuLocale))
            .findFirst().orElse(null);
        if (le == null) {
            return defaultUrl;
        }
        return le.url == null ? defaultUrl : "<a href=\"" + le.url + "\">" + le.url + "</a>";
    }

    private static String getLocalizedSpan(SisuLocale locale, String text) {
        String languageSpanStart = "<span lang=\"%lang%\" class=\"multilang\">";
        String languageSpanEnd = "</span>";
        return languageSpanStart.replace("%lang%", locale.toString().toLowerCase()) + text + languageSpanEnd;
    }

    private static String getMainOrganisationId(SisuCourseUnitRealisation cur) {
        if (cur.organisations == null || cur.organisations.isEmpty()) {
            return null;
        }
        return cur.organisations.stream()
            .filter(o -> RESPONSIBLE_ORGANISATION.equals(o.roleUrn) && o.share > 0.5)
            .findFirst()
            .map(o -> o.organisation.id)
            .orElse(null);
    }
}
