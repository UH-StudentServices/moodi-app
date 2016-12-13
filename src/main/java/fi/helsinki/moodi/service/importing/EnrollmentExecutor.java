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

import fi.helsinki.moodi.integration.esb.EsbService;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatus;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatusService;
import fi.helsinki.moodi.service.synchronize.log.LoggingService;
import fi.helsinki.moodi.service.util.MapperService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EnrollmentExecutor {

    private static final Logger LOGGER = getLogger(EnrollmentExecutor.class);

    private final MoodleService moodleService;
    private final EsbService esbService;
    private final MapperService mapperService;
    private final CourseEnrollmentStatusService courseEnrollmentStatusService;
    private final CourseService courseService;
    private final LoggingService loggingService;

    @Autowired
    public EnrollmentExecutor(
        MoodleService moodleService,
        EsbService esbService,
        MapperService mapperService,
        CourseEnrollmentStatusService courseEnrollmentStatusService,
        CourseService courseService,
        LoggingService loggingService) {
        this.moodleService = moodleService;
        this.esbService = esbService;
        this.mapperService = mapperService;
        this.courseEnrollmentStatusService = courseEnrollmentStatusService;
        this.courseService = courseService;
        this.loggingService = loggingService;
    }


    @Async("taskExecutor")
    public void processEnrollments(final Course course,
                                   final OodiCourseUnitRealisation courseUnitRealisation,
                                   final long moodleCourseId) {
        try {

            LOGGER.debug("Enrollment executor started for realisationId {} ", course.realisationId);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            final List<EnrollmentWarning> enrollmentWarnings = newArrayList();

            final List<Enrollment> enrollments = createEnrollments(courseUnitRealisation);

            final List<Enrollment> approvedEnrollments = filterApprovedEnrollments(enrollments, enrollmentWarnings);
            final List<Enrollment> enrollmentsWithUsernames = enrichEnrollmentsWithUsernames(approvedEnrollments);
            final List<Enrollment> enrollmentsWithMoodleIds = enrichEnrollmentsWithMoodleIds(enrollmentsWithUsernames);

            persistMoodleEnrollments(moodleCourseId, enrollmentsWithMoodleIds, enrollmentWarnings);

            CourseEnrollmentStatus courseEnrollmentStatus = courseEnrollmentStatusService.persistCourseEnrollmentStatus(
                course.id,
                course.realisationId,
                enrollmentsWithMoodleIds,
                enrollmentWarnings);

            courseService.completeCourseImport(course.realisationId, true);

            stopWatch.stop();

            loggingService.logCourseImportEnrollments(courseEnrollmentStatus);

            LOGGER.debug("Enrollment executor for realisationId {} finished in {} seconds", course.realisationId, stopWatch.getTotalTimeSeconds());

        } catch(Exception e) {
            courseService.completeCourseImport(course.realisationId, false);
            LOGGER.error("Enrollment execution failed for course " + course.realisationId);
            e.printStackTrace();
        }
    }

    private List<Enrollment> enrichEnrollmentsWithMoodleIds(final List<Enrollment> enrollments) {
        enrollments.stream()
                .forEach(e -> {
                    e.moodleId = moodleService.getUser(e.usernameList).map(user -> user.id);
                });

        return enrollments;
    }

    private List<Enrollment> enrichEnrollmentsWithUsernames(final List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(this::enrichEnrollmentWithUsername)
                .collect(toList());
    }

    private Enrollment enrichEnrollmentWithUsername(final Enrollment enrollment) {
        enrollment.usernameList = Enrollment.ROLE_TEACHER.equals(enrollment.role) ?
            esbService.getTeacherUsernameList(enrollment.teacherId.get()) :
            esbService.getStudentUsernameList(enrollment.studentNumber.get());

        return enrollment;
    }

    private List<Enrollment> createEnrollments(final OodiCourseUnitRealisation cur) {
        final List<Enrollment> enrollments = newArrayList();

        enrollments.addAll(cur.students.stream()
            .map(s -> Enrollment.forStudent(s.studentNumber, s.approved))
            .collect(toList()));

        enrollments.addAll(cur.teachers.stream()
            .map(s -> Enrollment.forTeacher(s.teacherId))
            .collect(toList()));

        return enrollments;
    }

    private void logMoodleEnrollments(final List<MoodleEnrollment> moodleEnrollments) {
        LOGGER.info("About to create {} enrollments to Moodle", moodleEnrollments.size());
        moodleEnrollments.forEach(e -> LOGGER.info(e.toString()));
    }

    private List<MoodleEnrollment> buildMoodleEnrollments(final long courseId, final List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(e -> new MoodleEnrollment(mapperService.getMoodleRole(e.role), e.moodleId.get(), courseId))
                .flatMap(enrollment -> Stream.of(
                    enrollment,
                    new MoodleEnrollment(mapperService.getMoodiRoleId(), enrollment.moodleUserId, enrollment.moodleCourseId)))
                .collect(toList());
    }

    private List<Enrollment> filterApprovedEnrollments(
        final List<Enrollment> enrollments,
        final List<EnrollmentWarning> enrollmentWarnings) {

        return enrollments
            .stream()
            .filter(e -> {
                if(!e.approved) {
                    enrollmentWarnings.add(EnrollmentWarning.userNotApproved(e));
                }
                return e.approved;
            })
            .collect(Collectors.toList());
    }

    private List<Enrollment> filterOutEnrollmentsWithoutUsername(
            final List<Enrollment> enrollments,
            final List<EnrollmentWarning> enrollmentWarnings) {

        return filterEnrollmentsAndCreateWarnings(
                enrollments, enrollmentWarnings, this::isUsernamePresent, EnrollmentWarning::userNotFoundFromEsb);
    }

    private List<Enrollment> filterOutEnrollmentsWithoutMoodleIds(
            final List<Enrollment> enrollments,
            final List<EnrollmentWarning> enrollmentWarnings) {

        return filterEnrollmentsAndCreateWarnings(
                enrollments, enrollmentWarnings, this::isMoodleIdPresent, EnrollmentWarning::userNotFoundFromMoodle);
    }

    private boolean isMoodleIdPresent(final Enrollment enrollment) {
        return enrollment.moodleId.isPresent();
    }

    private boolean isUsernamePresent(final Enrollment enrollment) {
        return enrollment.usernameList != null && enrollment.usernameList.size() > 0;
    }

    private List<Enrollment> filterEnrollmentsAndCreateWarnings(
            final List<Enrollment> enrollments,
            final List<EnrollmentWarning> enrollmentWarnings,
            final Predicate<Enrollment> partitionPredicate,
            final Function<Enrollment, EnrollmentWarning> warningCreator) {

        final Map<Boolean, List<Enrollment>> partitions =
                enrollments.stream().collect(partitioningBy(partitionPredicate));

        partitions.getOrDefault(false, newArrayList())
                .stream()
                .map(warningCreator)
                .forEach(enrollmentWarnings::add);

        return partitions.getOrDefault(true, newArrayList());
    }

    private long countEnrollmentsByRole(List<Enrollment> enrollments, String role) {
        return enrollments.stream().filter(e -> role.equals(e.role)).count();
    }

    private void persistMoodleEnrollments(final long courseId,
                                          final List<Enrollment> enrollments,
                                          final List<EnrollmentWarning> enrollmentWarnings) {

        final List<Enrollment> enrollmentsWithUsernames = filterOutEnrollmentsWithoutUsername(enrollments, enrollmentWarnings);
        final List<Enrollment> enrollmentsWithMoodleIds = filterOutEnrollmentsWithoutMoodleIds(enrollmentsWithUsernames, enrollmentWarnings);

        final long teacherCount = countEnrollmentsByRole(enrollmentsWithMoodleIds, Enrollment.ROLE_TEACHER);
        final long studentCount = countEnrollmentsByRole(enrollmentsWithMoodleIds, Enrollment.ROLE_STUDENT);

        LOGGER.info("About to enroll {} teacher(s) and {} students", teacherCount, studentCount);

        if (enrollToCourse(courseId, enrollmentsWithMoodleIds)) {
            LOGGER.info("Successfully enrolled {} teacher(s) and {} student(s)", teacherCount, studentCount);
        } else {
            LOGGER.warn("Failed to enroll {} teacher(s) and {} student(s)", teacherCount, studentCount);
            enrollmentsWithMoodleIds
                    .stream()
                    .map(EnrollmentWarning::enrollFailed)
                    .forEach(enrollmentWarnings::add);
        }
    }

    private boolean enrollToCourse(final long courseId, final List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) {
            return true;
        }

        try {
            final List<MoodleEnrollment> moodleEnrollments = buildMoodleEnrollments(courseId, enrollments);
            logMoodleEnrollments(moodleEnrollments);
            moodleService.addEnrollments(moodleEnrollments);
            return true;
        } catch (Exception e) {
            LOGGER.error("An error occurred while enrolling", e);
            return false;
        }
    }
}
