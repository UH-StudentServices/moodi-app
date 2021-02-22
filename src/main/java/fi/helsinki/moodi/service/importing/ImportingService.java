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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryService;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.converter.CourseConverter;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.dto.CourseDto;
import fi.helsinki.moodi.service.log.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fi.helsinki.moodi.exception.NotFoundException.notFoundException;
import static fi.helsinki.moodi.integration.studyregistry.StudyRegistryService.SISU_OODI_COURSE_PREFIX;
import static fi.helsinki.moodi.integration.studyregistry.StudyRegistryService.isOodiId;

@Service
public class ImportingService {

    private final MoodleService moodleService;
    private final CourseService courseService;
    private final StudyRegistryService studyRegistryService;
    private final CourseConverter courseConverter;
    private final MoodleCourseBuilder moodleCourseBuilder;
    private final EnrollmentExecutor enrollmentExecutor;
    private final LoggingService loggingService;

    @Autowired
    public ImportingService(
        MoodleService moodleService,
        CourseService courseService,
        StudyRegistryService studyRegistryService,
        CourseConverter courseConverter,
        MoodleCourseBuilder moodleCourseBuilder,
        EnrollmentExecutor enrollmentExecutor,
        LoggingService loggingService) {

        this.moodleService = moodleService;
        this.courseService = courseService;
        this.studyRegistryService = studyRegistryService;
        this.courseConverter = courseConverter;
        this.moodleCourseBuilder = moodleCourseBuilder;
        this.enrollmentExecutor = enrollmentExecutor;
        this.loggingService = loggingService;
    }

    public Result<ImportCourseResponse, String> importCourse(final ImportCourseRequest request) {
        final String sisuRealisationId = sisuRealisationId(request.realisationId);

        final Optional<Course> existingCourse = courseService.findByRealisationId(sisuRealisationId);

        if (existingCourse.isPresent()) {
            throw new CourseAlreadyCreatedException(sisuRealisationId);
        }

        final StudyRegistryCourseUnitRealisation courseUnitRealisation =
                studyRegistryService.getCourseUnitRealisation(sisuRealisationId)
                        .orElseThrow(notFoundException(
                            String.format("Study registry course not found with realisation id %s (%s)",
                                sisuRealisationId, request.realisationId)));

        final MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(courseUnitRealisation);
        final long moodleCourseId = moodleService.createCourse(moodleCourse);

        Course savedCourse = courseService.createCourse(sisuRealisationId, moodleCourseId);

        enrollmentExecutor.processEnrollments(savedCourse, courseUnitRealisation, moodleCourseId);

        loggingService.logCourseImport(savedCourse);

        return Result.success(new ImportCourseResponse(moodleCourseId));
    }

    // Does a straightforward conversion, which only works for Oodi native courses, not ones that are created in Optime.
    private String sisuRealisationId(String realisationId) {
        return isOodiId(realisationId) ? SISU_OODI_COURSE_PREFIX + realisationId
            : realisationId;
    }

    public CourseDto getCourse(String realisationId) {
        Optional<Course> course = courseService.findByRealisationId(realisationId);
        if (!course.isPresent() && isOodiId(realisationId)) {
            course = courseService.findByRealisationId(sisuRealisationId(realisationId));
        }
        return course
            .map(courseConverter::toDto)
            .orElseThrow(() -> new CourseNotFoundException(realisationId));
    }
}
