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

import com.google.common.base.Stopwatch;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.service.batch.BatchProcessor;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.log.LoggingService;
import fi.helsinki.moodi.service.synchronize.snapshot.CourseSyncParticipant;
import fi.helsinki.moodi.service.synchronize.snapshot.CourseSyncParticipantService;
import fi.helsinki.moodi.service.util.MapperService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EnrollmentService {

    private static final int ENROLLMENT_BATCH_MAX_SIZE = 300;

    private static final Logger logger = getLogger(EnrollmentService.class);

    private final MoodleService moodleService;
    private final MapperService mapperService;
    private final CourseService courseService;
    private final LoggingService loggingService;
    private final CourseSyncParticipantService courseSyncParticipantService;
    private final BatchProcessor<Enrollment> batchProcessor;

    @Autowired
    public EnrollmentService(
        MoodleService moodleService,
        MapperService mapperService,
        CourseService courseService,
        LoggingService loggingService,
        CourseSyncParticipantService courseSyncParticipantService,
        BatchProcessor batchProcessor) {
        this.moodleService = moodleService;
        this.mapperService = mapperService;
        this.courseService = courseService;
        this.loggingService = loggingService;
        this.courseSyncParticipantService = courseSyncParticipantService;
        this.batchProcessor = batchProcessor;
    }

    public void processEnrollments(final Course course,
                                   final StudyRegistryCourseUnitRealisation courseUnitRealisation,
                                   final long moodleCourseId) {
        try {

            logger.info("Process enrollments started for realisationId {} ", course.realisationId);

            final Stopwatch stopwatch = Stopwatch.createStarted();

            final List<EnrollmentWarning> enrollmentWarnings = newArrayList();

            final List<Enrollment> enrollments = createEnrollments(courseUnitRealisation, course.creatorUsername);

            final List<Enrollment> approvedEnrollments = filterApprovedEnrollments(enrollments, enrollmentWarnings);
            final List<Enrollment> enrollmentsWithMoodleIds = enrichEnrollmentsWithMoodleIds(approvedEnrollments);

            // Usernames coming from Sisu (students and teachers). The enrollments list also
            // contains the course creator, added by Moodi itself in createEnrollments, but
            // writeSyncSnapshots skips usernames not present in this set. This leaves the
            // creator out of the sync state snapshot: the creator is enrolled only to set up
            // the course and may be removed from it later, so the Sisu synchronization must
            // not manage (or re-add) that enrollment.
            final Set<String> sisuUsernames = Stream.concat(
                    courseUnitRealisation.students.stream().map(s -> s.userName),
                    courseUnitRealisation.teachers.stream().map(t -> t.userName))
                .filter(StringUtils::isNotEmpty)
                .collect(toSet());

            batchProcessor.process(
                enrollmentsWithMoodleIds,
                itemsToProcess -> persistMoodleEnrollments(course, moodleCourseId, itemsToProcess, enrollmentWarnings, sisuUsernames),
                ENROLLMENT_BATCH_MAX_SIZE);

            courseService.completeCourseImport(course.realisationId, true);
            // Snapshots for the successfully enrolled Sisu users were written per batch in
            // writeSyncSnapshots. Users in a failed batch have no snapshot row and are
            // self-healed by the next DELTA run (no snapshot -> idempotent ADD_ENROLLMENT),
            // so the course counts as bootstrapped even when some enrollments failed.
            courseService.markSyncBootstrapped(course.realisationId);

            loggingService.logCourseImportEnrollments(course, enrollmentsWithMoodleIds, enrollmentWarnings);

            logger.info("Process enrollments for realisationId {} finished in {}", course.realisationId, stopwatch.stop().toString());

        } catch (Exception e) {
            courseService.completeCourseImport(course.realisationId, false);
            logger.error("Processing enrollments failed for course " + course.realisationId, e);
        }
    }

    private List<Enrollment> enrichEnrollmentsWithMoodleIds(final List<Enrollment> enrollments) {
        enrollments.forEach(e -> e.moodleId = moodleService.getUser(e.usernameList).map(MoodleUser::getId));
        return enrollments;
    }

    private List<Enrollment> createEnrollments(final StudyRegistryCourseUnitRealisation cur, final String creatorUsername) {
        final List<Enrollment> enrollments = newArrayList();

        enrollments.addAll(cur.students.stream()
            .map(s -> Enrollment.forStudent(
                s.studentNumber,
                s.userName,
                s.isEnrolled))
            .toList());

        enrollments.addAll(cur.teachers.stream()
            .map(s -> Enrollment.forTeacher(s.employeeNumber, s.userName))
            .toList());

        if (creatorUsername != null) {
            enrollments.add(Enrollment.forCreator(creatorUsername));
        }

        return enrollments;
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

        return filterEnrollmentsAndCreateWarnings(
            enrollments, enrollmentWarnings, enrollment -> enrollment.approved, EnrollmentWarning::userNotApproved);
    }

    private List<Enrollment> filterOutEnrollmentsWithoutUsername(
            final List<Enrollment> enrollments,
            final List<EnrollmentWarning> enrollmentWarnings) {

        return filterEnrollmentsAndCreateWarnings(
                enrollments, enrollmentWarnings, this::isUsernamePresent, EnrollmentWarning::userWithoutUsername);
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
        return enrollment.usernameList != null && !enrollment.usernameList.isEmpty();
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

    private List<Enrollment> persistMoodleEnrollments(final Course course,
                                          final long moodleCourseId,
                                          final List<Enrollment> enrollments,
                                          final List<EnrollmentWarning> enrollmentWarnings,
                                          final Set<String> sisuUsernames) {

        final List<Enrollment> enrollmentsWithUsernames = filterOutEnrollmentsWithoutUsername(enrollments, enrollmentWarnings);
        final List<Enrollment> enrollmentsWithMoodleIds = filterOutEnrollmentsWithoutMoodleIds(enrollmentsWithUsernames, enrollmentWarnings);

        final long teacherCount = countEnrollmentsByRole(enrollmentsWithMoodleIds, Enrollment.ROLE_TEACHER);
        final long studentCount = countEnrollmentsByRole(enrollmentsWithMoodleIds, Enrollment.ROLE_STUDENT);

        logger.info("About to enroll {} teacher(s) and {} students", teacherCount, studentCount);

        if (enrollToCourse(moodleCourseId, enrollmentsWithMoodleIds)) {
            logger.info("Successfully enrolled {} teacher(s) and {} student(s)", teacherCount, studentCount);
            // Record the applied state so that delta-based synchronization can detect Sisu
            // changes that happen between the import and the first synchronization run.
            writeSyncSnapshots(course.id, enrollmentsWithMoodleIds, sisuUsernames);
        } else {
            logger.warn("Failed to enroll {} teacher(s) and {} student(s)", teacherCount, studentCount);
            enrollmentsWithMoodleIds
                    .stream()
                    .map(EnrollmentWarning::enrollFailed)
                    .forEach(enrollmentWarnings::add);
        }

        return enrollments;
    }

    private void writeSyncSnapshots(final Long courseId, final List<Enrollment> enrollments, final Set<String> sisuUsernames) {
        if (courseId == null) {
            return;
        }
        final Map<String, CourseSyncParticipant> rowsByUsername = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollments) {
            final String username = enrollment.usernameList.getFirst();
            if (!sisuUsernames.contains(username)) {
                continue;
            }
            final CourseSyncParticipant row = rowsByUsername.computeIfAbsent(username,
                u -> new CourseSyncParticipant(courseId, u, null, false, false, false));
            row.moodleUserId = enrollment.moodleId.get();
            if (Enrollment.ROLE_STUDENT.equals(enrollment.role)) {
                // Only approved (enrolled) students get this far in the import.
                row.student = true;
                row.enrolledStudent = true;
            } else if (Enrollment.ROLE_TEACHER.equals(enrollment.role)) {
                row.teacher = true;
            }
        }
        courseSyncParticipantService.upsertAll(courseId, new ArrayList<>(rowsByUsername.values()));
    }

    private boolean enrollToCourse(final long courseId, final List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) {
            return true;
        }

        try {
            final List<MoodleEnrollment> moodleEnrollments = buildMoodleEnrollments(courseId, enrollments);
            moodleService.addEnrollments(moodleEnrollments);
            return true;
        } catch (Exception e) {
            logger.error("An error occurred while enrolling", e);
            return false;
        }
    }
}
