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

package fi.helsinki.moodi.service.importing;

import fi.helsinki.moodi.exception.CourseAlreadyCreatedException;
import fi.helsinki.moodi.exception.CourseNotFoundException;
import fi.helsinki.moodi.integration.moodle.MoodleCourse;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiService;
import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.converter.CourseConverter;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.dto.CourseDto;
import fi.helsinki.moodi.service.log.LoggingService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fi.helsinki.moodi.exception.NotFoundException.notFoundException;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class ImportingService {

    private static final Logger LOGGER = getLogger(ImportingService.class);

    private final MoodleService moodleService;
    private final CourseService courseService;
    private final OodiService oodiService;
    private final CourseConverter courseConverter;
    private final MoodleCourseBuilder moodleCourseBuilder;
    private final EnrollmentExecutor enrollmentExecutor;
    private final LoggingService loggingService;

    @Autowired
    public ImportingService(
        MoodleService moodleService,
        CourseService courseService,
        OodiService oodiService,
        CourseConverter courseConverter,
        MoodleCourseBuilder moodleCourseBuilder,
        EnrollmentExecutor enrollmentExecutor,
        LoggingService loggingService) {

        this.moodleService = moodleService;
        this.courseService = courseService;
        this.oodiService = oodiService;
        this.courseConverter = courseConverter;
        this.moodleCourseBuilder = moodleCourseBuilder;
        this.enrollmentExecutor = enrollmentExecutor;
        this.loggingService = loggingService;
    }

    public Result<ImportCourseResponse, String> importCourse(final ImportCourseRequest request) {
        final Optional<Course> existingCourse = courseService.findByRealisationId(request.realisationId);

        if (existingCourse.isPresent()) {
            throw new CourseAlreadyCreatedException(request.realisationId);
        }

        final OodiCourseUnitRealisation courseUnitRealisation =
                oodiService.getOodiCourseUnitRealisation(request.realisationId)
                        .orElseThrow(notFoundException("Oodi course not found with realisation id " + request
                            .realisationId));

        final MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(courseUnitRealisation);
        final long moodleCourseId = moodleService.createCourse(moodleCourse);

        Course savedCourse = courseService.createCourse(request.realisationId, moodleCourseId);

        enrollmentExecutor.processEnrollments(savedCourse, courseUnitRealisation, moodleCourseId);

        loggingService.logCourseImport(savedCourse);

        return Result.success(new ImportCourseResponse(moodleCourseId));

    }

    public CourseDto getCourse(Long realisationId) {
        Optional<Course> course = courseService.findByRealisationId(realisationId);
        return course
            .map(courseConverter::toDto)
            .orElseThrow(() -> new CourseNotFoundException(realisationId));
    }
}
