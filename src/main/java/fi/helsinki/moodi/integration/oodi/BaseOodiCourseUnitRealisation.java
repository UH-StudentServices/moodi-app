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

package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

public class BaseOodiCourseUnitRealisation {

    @JsonProperty("course_id")
    public Integer realisationId;

    @JsonProperty("end_date")
    public String endDate;

    @JsonProperty("start_date")
    public String startDate;

    @JsonProperty("deleted")
    public boolean removed;

    @JsonProperty("students")
    public List<OodiStudent> students = newArrayList();

    @JsonProperty("teachers")
    public List<OodiTeacher> teachers = newArrayList();

    public StudyRegistryCourseUnitRealisation toStudyRegistryCourseUnitRealisation() {
        StudyRegistryCourseUnitRealisation ret = new StudyRegistryCourseUnitRealisation();
        ret.origin = StudyRegistryCourseUnitRealisation.Origin.OODI;
        ret.realisationId = "" + realisationId;
        ret.startDate = parseDateTime(startDate, LocalDate.now());
        ret.endDate = parseDateTime(endDate, LocalDate.now().plusMonths(11)).plusMonths(1);
        ret.published = !removed;
        ret.students = students.stream().map(OodiStudent::toStudyRegistryStudent).collect(Collectors.toList());
        ret.teachers = teachers.stream().map(OodiTeacher::toStudyRegistryTeacher).collect(Collectors.toList());

        return ret;
    }

    private LocalDate parseDateTime(String s, LocalDate defaultValue) {
        try {
            return ZonedDateTime.parse(s).withZoneSameInstant(ZoneId.of("EET")).toLocalDate();
        } catch (Exception e) {
            // Fall through and return defaultValue
        }
        return defaultValue;
    }
}
