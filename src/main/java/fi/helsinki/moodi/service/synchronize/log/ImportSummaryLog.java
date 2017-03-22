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

package fi.helsinki.moodi.service.synchronize.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.helsinki.moodi.service.importing.Enrollment;
import fi.helsinki.moodi.service.importing.EnrollmentWarning;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.service.importing.Enrollment.ROLE_STUDENT;
import static fi.helsinki.moodi.service.importing.Enrollment.ROLE_TEACHER;

public class ImportSummaryLog {
    private final List<Enrollment> enrollments;
    private final List<EnrollmentWarning> enrollmentWarnings;
    private final List<Enrollment> successfulEnrollments;
    private final List<Enrollment> failedEnrollments;

    public final EnrollmentSummary summary;
    public final List<EnrollmentEntry> enrolledStudents;
    public final List<EnrollmentEntry> enrolledTeachers;
    public final Map<String, List<EnrollmentEntry>> failedStudents;
    public final Map<String, List<EnrollmentEntry>> failedTeachers;

    public ImportSummaryLog(List<Enrollment> enrollments, List<EnrollmentWarning> enrollmentWarnings) {
        this.enrollments = enrollments;
        this.enrollmentWarnings = enrollmentWarnings;
        this.failedEnrollments = enrollmentWarnings.stream().map(e -> e.enrollment).collect(Collectors.toList());
        this.successfulEnrollments = enrollments.stream().filter(e -> !failedEnrollments.contains(e)).collect(Collectors.toList());

        this.summary = createSummary();

        this.enrolledStudents = getSuccessfulEnrollmentEntries(ROLE_STUDENT, this::createStudentEnrollmentEntry);
        this.enrolledTeachers = getSuccessfulEnrollmentEntries(ROLE_TEACHER, this::createTeacherEnrollmentEntry);

        this.failedStudents = getFailedEnrollmentEntries(ROLE_STUDENT, this::createStudentEnrollmentEntry);
        this.failedTeachers = getFailedEnrollmentEntries(ROLE_TEACHER, this::createTeacherEnrollmentEntry);

    }

    private List<EnrollmentEntry> getSuccessfulEnrollmentEntries(String role, Function<Enrollment, EnrollmentEntry> createEnrollmentEntry) {
        return successfulEnrollments.stream()
            .filter(e -> role.equals(e.role))
            .map(createEnrollmentEntry::apply)
            .collect(Collectors.toList());
    }

    private Map<String, List<EnrollmentEntry>> getFailedEnrollmentEntries(String role, Function<Enrollment, EnrollmentEntry> createEnrollmentEntry) {
        return enrollmentWarnings.stream()
            .filter(w -> role.equals(w.enrollment.role))
            .map(w -> {
                EnrollmentEntry failedEnrollmentEntry = createEnrollmentEntry.apply(w.enrollment);
                failedEnrollmentEntry.message = w.code;
                return failedEnrollmentEntry;
            })
            .collect(Collectors.groupingBy(e -> e.message));
    }


    private EnrollmentSummary createSummary() {
        final long studentCount = successfulEnrollments.stream()
            .filter(e -> ROLE_STUDENT.equals(e.role)).count();
        final long teacherCount = successfulEnrollments.stream()
            .filter(e -> ROLE_TEACHER.equals(e.role)).count();

        final Map<String, Long> failedStudentCount = getReasonCountMapForRole(ROLE_STUDENT);
        final Map<String, Long> failedTeacherCount = getReasonCountMapForRole(ROLE_TEACHER);

        return new EnrollmentSummary(studentCount, teacherCount, failedStudentCount, failedTeacherCount);
    }

    private Map<String, Long> getReasonCountMapForRole(String role) {
        return enrollmentWarnings.stream()
            .filter(w -> role.equals(w.enrollment.role)).collect(Collectors.groupingBy(w -> w.code, Collectors.counting()));
    }

    private EnrollmentEntry createStudentEnrollmentEntry(Enrollment enrollment) {
        return new StudentEnrollmentEntry(enrollment);
    }

    private EnrollmentEntry createTeacherEnrollmentEntry(Enrollment enrollment) {
        return new TeacherEnrollmentEntry(enrollment);
    }

    public static class StudentEnrollmentEntry extends EnrollmentEntry {
        public final String studentNumber;

        public StudentEnrollmentEntry(Enrollment enrollment) {
            this.studentNumber = enrollment.studentNumber.orElseThrow(() -> new RuntimeException("No studentNumber found for student Enrollment"));
        }
    }

    public static class TeacherEnrollmentEntry extends EnrollmentEntry {
        public final String teacherId;

        public TeacherEnrollmentEntry(Enrollment enrollment) {
            this.teacherId = enrollment.teacherId.orElseThrow(() -> new RuntimeException("No teacherId found for teacher Enrollment"));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static abstract class EnrollmentEntry {
        public String message;
    }

    public static class EnrollmentSummary {
        public final long enrolledStudents;
        public final long enrolledTeachers;
        public final Map<String, Long> failedStudents;
        public final Map<String, Long> failedTeachers;

        public EnrollmentSummary(long enrolledStudents,
                                 long enrolledTeachers,
                                 Map<String, Long> failedStudents,
                                 Map<String, Long> failedTeachers) {
            this.enrolledStudents = enrolledStudents;
            this.enrolledTeachers = enrolledTeachers;
            this.failedStudents = failedStudents;
            this.failedTeachers = failedTeachers;
        }
    }
}
