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
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.process.EnrollmentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonIgnoreProperties("fullSummary")
public class SynchronizationSummaryLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationSummaryLog.class);

    private final SynchronizationSummary fullSummary;

    public SynchronizationSummaryLog(SynchronizationSummary fullSummary) {
        this.fullSummary = fullSummary;
    }

    public Object getSyncronizationSummary() {
        return new Object() {
            public String elapsedTime = fullSummary.getElapsedTime();
            public String type = fullSummary.getType().name();
            public long successfulItemsCount = fullSummary.getSuccessfulItemsCount();
            public long failedItemsCount = fullSummary.getFailedItemsCount();
            public List<Object> courses = fullSummary.getItems()
                .stream()
                .map(item -> mapItem(item))
                .collect(Collectors.toList());
        };
    }

    private Object mapItem(SynchronizationItem item) {
        try {
            return new Object() {
                public long realisationId = Optional.ofNullable(item.getCourse()).map(course -> course.realisationId).orElse(null);
                public Long moodleId = Optional.ofNullable(item.getCourse()).map(course -> course.moodleId).orElse(null);
                public String enrichmentStatus = item.getEnrichmentStatus().name();
                public String processingStatus = item.getProcessingStatus().name();

                public Object students = getEnrollmentDetails(
                    item.getStudentItems().map(SynchronizationSummaryLog::getStudentSynchronizationItemLogEntries).orElse(null));

                public Object teachers = getEnrollmentDetails(
                    item.getTeacherItems().map(SynchronizationSummaryLog::getTeacherSynchronizationItemLogEntries).orElse(null));

                public List<MoodleUserEnrollments> moodleEnrollments = item.getMoodleEnrollments().orElse(null);
                public String message = item.getMessage();
            };
        } catch (Exception e) {
            LOGGER.error("Could not create log entry for synchronizationItem", e);
            return null;
        }
    }

    private static <S extends SyncronizationItemLogEntry> Object getEnrollmentDetails(List<S> items) {
        if(items != null) {
            return new Object() {
                public Map<String, Long> summary = getEnrollmentDetailsSummary(items);
                public Map<String, List<S>> results = getEnrollmentResults(items);
            };
        } else {
            return null;
        }
    }

    private static <S extends SyncronizationItemLogEntry> Map<String, Long> getEnrollmentDetailsSummary(List<S> items) {
        return items.stream().collect(Collectors.groupingBy(S::getMessage, Collectors.counting()));
    }

    private static <S extends SyncronizationItemLogEntry> Map<String, List<S>> getEnrollmentResults(List<S> items) {
        return items.stream().collect(Collectors.groupingBy(S::getMessage));
    }

    private static <T extends EnrollmentSynchronizationItem> List<Object> getSynchronizationItemLogEntries(List<T> items,
                                                                 Function<T, Object> logEntryBuilder) {
        return items
            .stream()
            .map(item -> {
                try {
                    return logEntryBuilder.apply(item);
                } catch (Exception e) {
                    LOGGER.error("Could not create log entry for enrolmentSynchronizationItem", e);
                    return null;
                }
            })
            .collect(Collectors.toList());
    }

    private static List<StudentSyncronizationItemLogEntry> getStudentSynchronizationItemLogEntries(List<StudentSynchronizationItem> items) {
        return items.stream().map(StudentSyncronizationItemLogEntry::new).collect(Collectors.toList());
    }

    private static List<TeacherSyncronizationItemLogEntry> getTeacherSynchronizationItemLogEntries(List<TeacherSynchronizationItem> items) {
        return items.stream().map(TeacherSyncronizationItemLogEntry::new).collect(Collectors.toList());
    }

    private static class StudentSyncronizationItemLogEntry extends SyncronizationItemLogEntry {

        public final String studentNumber;
        public final boolean approved;

        public StudentSyncronizationItemLogEntry(StudentSynchronizationItem item) {
            super(item);
            studentNumber = item.getStudent().studentNumber;
            approved = item.getStudent().approved;
        }
    }

    private static class TeacherSyncronizationItemLogEntry extends SyncronizationItemLogEntry {

        public final String teacherId;

        public TeacherSyncronizationItemLogEntry(TeacherSynchronizationItem item) {
            super(item);
            teacherId = item.getTeacher().teacherId;
        }
    }

    private static class SyncronizationItemLogEntry {
        private final String status;
        private final String message;
        private final List<MoodleRole> moodleRoles;
        private final Long moodleUserId;

        private SyncronizationItemLogEntry(EnrollmentSynchronizationItem item) {
            this.status = item.getEnrollmentSynchronizationStatus().name();
            this.message = item.getMessage();
            this.moodleRoles = item.getMoodleEnrollments().map(enrollment -> enrollment.roles).orElse(null);
            this.moodleUserId = item.getMoodleUser().map(user -> user.id).orElse(null);
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public List<MoodleRole> getMoodleRoles() {
            return moodleRoles;
        }

        public Long getMoodleUserId() {
            return moodleUserId;
        }
    }
}
