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
import java.util.function.Function;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.Constants.RESPONSIBLE_ORGANISATION;
import static fi.helsinki.moodi.Constants.TEACHER_TYPES;

public final class SisuCourseUnitRealisationConverter {

    public static final String SEPARATOR = ", ";

    private static final List<SisuLocale> FALLBACK_ORDER = List.of(SisuLocale.FI, SisuLocale.EN, SisuLocale.SV);

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

    private static String resolveText(SisuLocale preferred, Function<SisuLocale, String> getter) {
        String result = getter.apply(preferred);
        if (StringUtils.isNotBlank(result)) {
            return result;
        }
        for (SisuLocale fallback : FALLBACK_ORDER) {
            result = getter.apply(fallback);
            if (StringUtils.isNotBlank(result)) {
                return result;
            }
        }
        return null;
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

        String resolvedUrl = resolveText(teachingLanguageCode,
            locale -> getLearningEnvironmentUrl(cur.learningEnvironments, locale, null));
        if (resolvedUrl == null) {
            resolvedUrl = "";
        }

        String resolvedCUName = resolveText(teachingLanguageCode, COURSE_UNIT_LOCALIZATION::get);
        if (resolvedCUName == null) {
            resolvedCUName = "";
        }

        String courseUnitCodes = cur.courseUnits.stream()
            .sorted(Comparator.comparing(cu -> cu.code))
            .map(cu -> cu.code)
            .collect(Collectors.joining(", "));

        String resolvedCUType = resolveText(teachingLanguageCode,
            locale -> cur.courseUnitRealisationType.name.getForLocale(locale));
        if (resolvedCUType == null) {
            resolvedCUType = "";
        }

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

        ret.description = "<p>" + resolvedUrl + "</p>"
            + "<p>" + resolvedCUName + " " + courseUnitCodes + "</p>"
            + "<p>" + resolvedCUType + courseDatesPart + "</p>";

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
        return resolveText(teachingLanguage, locale -> calculateName(cur, locale));
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

    private static String getLearningEnvironmentUrl(List<SisuLearningEnvironment> learningEnvironments, SisuLocale sisuLocale, String defaultUrl) {
        SisuLearningEnvironment le = learningEnvironments.stream()
            .filter(x -> SisuLocale.byCodeOrDefaultToFi(x.language).equals(sisuLocale))
            .findFirst().orElse(null);
        if (le == null) {
            return defaultUrl;
        }
        return le.url == null ? defaultUrl : "<a href=\"" + le.url + "\">" + le.url + "</a>";
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
