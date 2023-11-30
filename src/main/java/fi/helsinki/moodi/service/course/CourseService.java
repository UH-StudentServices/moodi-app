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

import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.exception.NotFoundException.notFoundException;
import static fi.helsinki.moodi.service.course.Course.ImportStatus.*;

@Service
@Transactional
public class CourseService {

    private static final int MAX_IMPORT_TIME_SECONDS = 7200;

    private final CourseRepository courseRepository;
    private final TimeService timeService;
    private final MoodleService moodleService;

    @Autowired
    public CourseService(CourseRepository courseRepository, TimeService timeService, MoodleService moodleService) {
        this.courseRepository = courseRepository;
        this.timeService = timeService;
        this.moodleService = moodleService;
    }

    public void markAsRemoved(Course course, String message) {
        course.removed = true;
        course.removedMessage = message;
        saveCourse(course);
    }

    public Optional<Course> findByRealisationId(final String realisationId) {
        return courseRepository.findByRealisationId(realisationId);
    }

    public List<Course> findAllCompletedWithMoodleId() {
        return courseRepository.findByImportStatusInAndRemovedFalseAndMoodleIdNotNull(newArrayList(COMPLETED, COMPLETED_FAILED));
    }

    public Course createCourse(final String realisationId, final Long moodleCourseId, final String creatorUsername) {
        final Course course = new Course();
        course.created = timeService.getCurrentUTCDateTime();
        course.moodleId = moodleCourseId;
        course.realisationId = realisationId;
        course.importStatus = IN_PROGRESS;
        course.creatorUsername = creatorUsername;
        return saveCourse(course);
    }

    public Course updateMoodleId(final String realisationId, final long moodleId) {
        Course dbCourse = findByRealisationId(realisationId).orElseThrow(notFoundException(
                String.format("Study registry course not found with realisation id %s",
                        realisationId)));

        dbCourse.moodleId = moodleId;
        return saveCourse(dbCourse);
    }

    public Course completeCourseImport(String realisationId, boolean success) {
        Optional<Course> courseOptional = courseRepository.findByRealisationId(realisationId);

        Course course = courseOptional
            .orElseThrow(() -> new RuntimeException("Course not found to set import status for realisationId " + realisationId));

        course.importStatus = success ? COMPLETED : COMPLETED_FAILED;

        return saveCourse(course);
    }

    public List<Course> findCompletedWithMoodleIdByRealisationIds(List<String> realisationIds) {
        return courseRepository.findByImportStatusInAndRealisationIdInAndMoodleIdNotNull(newArrayList(COMPLETED, COMPLETED_FAILED), realisationIds);
    }

    public void cleanImportStatuses() {
        List<Course> inProgressCourses = courseRepository.findByImportStatusInAndRemovedFalse(newArrayList(IN_PROGRESS));

        inProgressCourses
            .stream()
            .filter(this::isImportExpired)
            .forEach(c -> {
                c.importStatus = COMPLETED_FAILED;
                saveCourse(c);
            });
    }

    private boolean isImportExpired(Course course) {
        return course.created
            .plusSeconds(MAX_IMPORT_TIME_SECONDS)
            .isBefore(timeService.getCurrentUTCDateTime());
    }

    private Course saveCourse(Course course) {
        course.modified = timeService.getCurrentUTCDateTime();
        return courseRepository.save(course);
    }

    public boolean ensureCourseVisibility(String realisationId) {
        Course dbCourse = findByRealisationId(realisationId).orElseThrow(notFoundException(
            String.format("Study registry course not found with realisation id %s",
                realisationId)));
        return moodleService.updateCourseVisibility(dbCourse.moodleId, true) != null;
    }

    public void deleteCourse(long id) {
        courseRepository.deleteById(id);
    }
}
