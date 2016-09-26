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

package fi.helsinki.moodi.service.synchronize.log.synchronizationSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.process.EnrollmentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonIgnoreProperties("fullSummary")
public class SynchronizationSummaryLog {
    private final SynchronizationSummary fullSummary;

    public SynchronizationSummaryLog(SynchronizationSummary fullSummary) {
        this.fullSummary = fullSummary;
    }

    public Object getSyncronizationSummary() {
        return new Object() {
            public String elapsedTime = fullSummary.getElapsedTime();
            public String type = fullSummary.getType().name();
            public List<Object> courses = fullSummary.getItems()
                .stream()
                .map(item -> mapItem(item))
                .collect(Collectors.toList());
        };
    }

    private Object mapItem(SynchronizationItem item) {
        return new Object() {
            public long realisationId = Optional.ofNullable(item.getCourse()).map(course -> course.realisationId).orElse(null);
            public String enrichmentStatus = item.getEnrichmentStatus().name();
            public String processingStatus = item.getProcessingStatus().name();
            public List<Object> syncedStudents = item.getStudentItems().map(SynchronizationSummaryLog::getSyncedStudents).orElse(null);
            public List<Object> syncedTeachers = item.getTeacherItems().map(SynchronizationSummaryLog::getSyncedTeachers).orElse(null);
        };
    }

    private static List<Object> getSyncedStudents(List<StudentSynchronizationItem> items) {
        return items
            .stream()
            .map(item ->
                new SyncronizationItemLogEntry(item) {
                    public String studentNumber = Optional.ofNullable(item.getStudent())
                        .map(student -> student.studentNumber)
                        .orElse(null);
                })
            .collect(Collectors.toList());
    }

    private static List<Object> getSyncedTeachers(List<TeacherSynchronizationItem> items) {
        return items
            .stream()
            .map(item ->
                new SyncronizationItemLogEntry(item) {
                    public String teacherId = Optional.ofNullable(item.getTeacher())
                        .map(teacher -> teacher.teacherId)
                        .orElse(null);
                })
            .collect(Collectors.toList());
    }

    private static class SyncronizationItemLogEntry {
        private final String status;
        private final String message;

        private SyncronizationItemLogEntry(EnrollmentSynchronizationItem item) {
            this.status = item.getEnrollmentSynchronizationStatus().name();
            this.message = item.getMessage();
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
