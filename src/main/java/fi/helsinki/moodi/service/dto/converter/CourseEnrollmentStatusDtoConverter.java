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

package fi.helsinki.moodi.service.dto.converter;


import com.fasterxml.jackson.core.type.TypeReference;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatus;
import fi.helsinki.moodi.service.dto.CourseEnrollmentStatusDto;
import fi.helsinki.moodi.service.dto.StudentEnrollmentStatusDto;
import fi.helsinki.moodi.service.dto.TeacherEnrollmentStatusDto;
import fi.helsinki.moodi.service.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseEnrollmentStatusDtoConverter {

    private final JsonUtil jsonUtil;

    @Autowired
    public CourseEnrollmentStatusDtoConverter(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    public CourseEnrollmentStatusDto toDto(CourseEnrollmentStatus courseEnrollmentStatus) {
        CourseEnrollmentStatusDto courseEnrollmentStatusDto = new CourseEnrollmentStatusDto();
        courseEnrollmentStatusDto.created = courseEnrollmentStatus.created;
        try {
            courseEnrollmentStatusDto.studentEnrollments =
                jsonUtil.getObjectMapper().readValue(courseEnrollmentStatus.studentEnrollments, new TypeReference<List<StudentEnrollmentStatusDto>>() {});
            courseEnrollmentStatusDto.teacherEnrollments =
                jsonUtil.getObjectMapper().readValue(courseEnrollmentStatus.teacherEnrollments, new TypeReference<List<TeacherEnrollmentStatusDto>>() {});
        } catch (Exception e) {}

        return courseEnrollmentStatusDto;

    }

}
