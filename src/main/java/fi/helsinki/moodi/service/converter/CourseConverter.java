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

package fi.helsinki.moodi.service.converter;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.dto.CourseDto;
import fi.helsinki.moodi.service.dto.CourseEnrollmentStatusDto;
import fi.helsinki.moodi.service.util.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CourseConverter {

    private final UrlBuilder urlBuilder;

    @Autowired
    public CourseConverter(UrlBuilder urlBuilder) {
        this.urlBuilder = urlBuilder;
    }

    public CourseDto toDto(Course course, CourseEnrollmentStatusDto courseEnrollmentStatus) {
        CourseDto courseDto = new CourseDto();
        courseDto.url = urlBuilder.getMoodleCourseUrl(course.moodleId);
        courseDto.importStatus = course.importStatus.toString();
        courseDto.courseEnrollmentStatus = courseEnrollmentStatus;
        return courseDto;
    }
}
