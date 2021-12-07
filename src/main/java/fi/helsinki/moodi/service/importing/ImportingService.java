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
import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryService;
import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.converter.CourseConverter;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.dto.CourseDto;
import fi.helsinki.moodi.service.log.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

import static fi.helsinki.moodi.exception.NotFoundException.notFoundException;

@Service
public class ImportingService {

    private final MoodleService moodleService;
    private final CourseService courseService;
    private final StudyRegistryService studyRegistryService;
    private final CourseConverter courseConverter;
    private final MoodleCourseBuilder moodleCourseBuilder;
    private final EnrollmentExecutor enrollmentExecutor;
    private final LoggingService loggingService;
    private final SisuClient sisuClient;

    @Autowired
    public ImportingService(
        MoodleService moodleService,
        CourseService courseService,
        StudyRegistryService studyRegistryService,
        CourseConverter courseConverter,
        MoodleCourseBuilder moodleCourseBuilder,
        EnrollmentExecutor enrollmentExecutor,
        LoggingService loggingService,
        SisuClient sisuClient) {

        this.moodleService = moodleService;
        this.courseService = courseService;
        this.studyRegistryService = studyRegistryService;
        this.courseConverter = courseConverter;
        this.moodleCourseBuilder = moodleCourseBuilder;
        this.enrollmentExecutor = enrollmentExecutor;
        this.loggingService = loggingService;
        this.sisuClient = sisuClient;
    }

    public Result<ImportCourseResponse, String> importCourse(final ImportCourseRequest request) {
        final Optional<Course> existingCourse = courseService.findByRealisationId(request.realisationId);

        Course dbCourse;
        // Check for moodleId. If the course exists in the DB without moodleId, go ahead with the import without inserting the course to DB.
        if (existingCourse.isPresent()) {
            dbCourse = existingCourse.get();
            if (dbCourse.moodleId != null) {
                throw new CourseAlreadyCreatedException(request.realisationId);
            }
        } else {
            // Save the course in the DB with moodleId NULL.
            dbCourse = courseService.createCourse(request.realisationId, null, getUserNameOrThrow(request.creatorSisuId));
        }

        final StudyRegistryCourseUnitRealisation courseUnitRealisation =
                studyRegistryService.getSisuCourseUnitRealisation(request.realisationId)
                        .orElseThrow(notFoundException(
                            String.format("Study registry course not found with realisation id %s (%s)",
                                request.realisationId, request.realisationId)));

        // Use the DB Course.id for generating the shortname.
        final MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(courseUnitRealisation, dbCourse.id);

        final long moodleCourseId = moodleService.createCourse(moodleCourse);

        // Update the course in DB with the newly created moodleId
        dbCourse = courseService.updateMoodleId(request.realisationId, moodleCourseId);

        // If this fails, the course gets created in Moodle without users, and sync will later try and put them in place.
        enrollmentExecutor.processEnrollments(dbCourse, courseUnitRealisation, moodleCourseId);

        loggingService.logCourseImport(dbCourse);

        return Result.success(new ImportCourseResponse(moodleCourseId));
    }

    private String getUserNameOrThrow(String creatorSisuId) {
        return creatorSisuId != null ?
            sisuClient.getPersons(Collections.singletonList(creatorSisuId)).stream()
                .findFirst()
                .orElseThrow(notFoundException(String.format("Sisu person not found with id %s", creatorSisuId)))
                .eduPersonPrincipalName :
            null;
    }

    public CourseDto getImportedCourse(String realisationId) {
        Optional<Course> course = courseService.findByRealisationId(realisationId);
        return course
            .filter(c -> c.moodleId != null)
            .map(courseConverter::toDto)
            .orElseThrow(() -> new CourseNotFoundException(realisationId));
    }
}
