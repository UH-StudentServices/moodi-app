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

package fi.helsinki.moodi.service.course.group;

import fi.helsinki.moodi.exception.CourseNotFoundException;
import fi.helsinki.moodi.exception.MoodleCourseNotFoundException;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Service
public class GroupSynchronizationService {

    private final CourseService courseService;
    private final MoodleService moodleService;
    private final SisuClient sisuClient;

    public GroupSynchronizationService(CourseService courseService, MoodleService moodleService, SisuClient sisuClient) {
        this.courseService = courseService;
        this.moodleService = moodleService;
        this.sisuClient = sisuClient;
    }

    // TODO: MOODI-168
    public SynchronizeGroupsResponse synchronizeGroups(@NotNull String realisationId) {
        final Optional<SisuCourseUnitRealisation> existingRealisation = sisuClient.getCourseUnitRealisation(realisationId);
        if (!existingRealisation.isPresent()) {
            throw new CourseNotFoundException(realisationId);
        }
        SisuCourseUnitRealisation realisation = existingRealisation.get();
        final Optional<Course> existingCourse = courseService.findByRealisationId(realisationId);
        if (!existingCourse.isPresent()) {
            throw new CourseNotFoundException(realisationId);
        }

        final Course course = existingCourse.get();
        final Optional<MoodleFullCourse> moodleCourse = moodleService.getCourse(course.moodleId);
        if (!moodleCourse.isPresent()) {
            throw new MoodleCourseNotFoundException(course.moodleId);
        }

        return new SynchronizeGroupsResponse(realisation);
    }
}
