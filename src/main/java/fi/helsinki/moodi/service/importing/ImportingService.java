package fi.helsinki.moodi.service.importing;

import fi.helsinki.moodi.exception.CourseAlreadyCreatedException;
import fi.helsinki.moodi.integration.moodle.MoodleCourse;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiService;
import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.converter.CourseConverter;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatusService;
import fi.helsinki.moodi.service.dto.CourseDto;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

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
    private final CourseEnrollmentStatusService courseEnrollmentStatusService;
    private final MoodleCourseBuilder moodleCourseBuilder;
    private final EnrollmentExecutor enrollmentExecutor;

    @Autowired
    public ImportingService(
        MoodleService moodleService,
        CourseService courseService,
        OodiService oodiService,
        CourseConverter courseConverter,
        CourseEnrollmentStatusService courseEnrollmentStatusService,
        MoodleCourseBuilder moodleCourseBuilder,
        EnrollmentExecutor enrollmentExecutor) {

        this.moodleService = moodleService;
        this.courseService = courseService;
        this.oodiService = oodiService;
        this.courseConverter = courseConverter;
        this.courseEnrollmentStatusService = courseEnrollmentStatusService;
        this.moodleCourseBuilder = moodleCourseBuilder;
        this.enrollmentExecutor = enrollmentExecutor;
    }

    public Result<ImportCourseResponse, String> importCourse(final ImportCourseRequest request) {

        LOGGER.info("Create course started with course realisation id {}", request.realisationId);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final Optional<Course> existingCourse = courseService.findByRealisationId(request.realisationId);
        if (existingCourse.isPresent()) {
            LOGGER.info("Course already created with realisation id {}", request.realisationId);
            throw new CourseAlreadyCreatedException(request.realisationId);
        }

        final OodiCourseUnitRealisation courseUnitRealisation =
                oodiService.getOodiCourseUnitRealisation(request.realisationId)
                        .orElseThrow(notFoundException("Oodi course not found with realisation id " + request
                            .realisationId));

        logOodiCourse(courseUnitRealisation);

        final MoodleCourse moodleCourse = moodleCourseBuilder.buildMoodleCourse(courseUnitRealisation);
        final long moodleCourseId = createMoodleCourse(moodleCourse);

        Course savedCourse = courseService.createCourse(request.realisationId, moodleCourseId);

        enrollmentExecutor.processEnrollments(savedCourse, courseUnitRealisation, moodleCourseId);

        stopWatch.stop();

        LOGGER.info("Course created for realisation id {} in {} seconds", request.realisationId, stopWatch.getTotalTimeSeconds());

        return Result.success(new ImportCourseResponse(moodleCourseId));

    }

    private void logMoodleCourse(final MoodleCourse moodleCourse) {
        LOGGER.info("About to create course to Moodle:\n{}", moodleCourse);
    }

    private void logOodiCourse(final OodiCourseUnitRealisation cur) {
        LOGGER.debug("Got course realisation from Oodi:\n{}", cur);
        LOGGER.debug("Number of students: {}", cur.students.size());
        LOGGER.debug("Number of teachers: {}", cur.teachers.size());
    }

    private long createMoodleCourse(final MoodleCourse moodleCourse) {
        logMoodleCourse(moodleCourse);
        return moodleService.createCourse(moodleCourse);
    }

    public CourseDto getCourse(Long realisationId) {
        Optional<Course> course = courseService.findByRealisationId(realisationId);
        return course
            .map(c -> courseConverter.toDto(c, courseEnrollmentStatusService.getCourseEnrollmentStatus(realisationId)))
            .orElseThrow(notFoundException("Course not found in Moodi"));
    }
}
