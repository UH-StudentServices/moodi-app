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

package fi.helsinki.moodi.service.course;

import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.course.Course.ImportStatus.*;

@Service
@Transactional
public class CourseService {

    private static final int MAX_IMPORT_TIME_SECONDS = 7200;

    private final CourseRepository courseRepository;
    private final TimeService timeService;

    @Autowired
    public CourseService(CourseRepository courseRepository, TimeService timeService) {
        this.courseRepository = courseRepository;
        this.timeService = timeService;
    }

    public void delete(final long courseId) {
        courseRepository.delete(courseId);
    }

    public Optional<Course> findByRealisationId(final long realisationId) {
        return courseRepository.findByRealisationId(realisationId);
    }

    public List<Course> findAllCompleted() {
        return courseRepository.findByImportStatusIn(newArrayList(COMPLETED, COMPLETED_FAILED));
    }

    public Course createCourse(final long realisationId, final long moodleCourseId) {
        final Course course = new Course();
        course.created = timeService.getCurrentDateTime();
        course.moodleId = moodleCourseId;
        course.realisationId = realisationId;
        course.importStatus = IN_PROGRESS;
        return courseRepository.save(course);
    }

    public Course completeCourseImport(long realisationId, boolean success) {
        Optional<Course> courseOptional = courseRepository.findByRealisationId(realisationId);

        Course course = courseOptional
            .orElseThrow(() -> new RuntimeException("Course not found to set import status for realisationId " + realisationId));

        course.importStatus = success ? COMPLETED : COMPLETED_FAILED;

        return courseRepository.save(course);
    }

    public List<Course> findCompletedByRealisationIds(List<Long> realisationIds) {
        return courseRepository.findByImportStatusInAndRealisationIdIn(newArrayList(COMPLETED, COMPLETED_FAILED), realisationIds);
    }

    public void cleanImportStatuses() {
        List<Course> inProgressCourses = courseRepository.findByImportStatusIn(newArrayList(IN_PROGRESS));

        inProgressCourses
            .stream()
            .filter(this::isImportExpired)
            .forEach(c -> {
                c.importStatus = COMPLETED_FAILED;
                courseRepository.save(c);
            });
    }

    private boolean isImportExpired(Course course) {
        return course.created
            .plusSeconds(MAX_IMPORT_TIME_SECONDS)
            .isBefore(timeService.getCurrentDateTime());
    }
}
