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

                public List<Object> syncedStudents = item
                    .getStudentItems()
                    .map(studentItems ->
                        getSynchronizationItemLogEntries(studentItems,
                            SynchronizationSummaryLog::getStudentSynchronizationItemLogEntry))
                    .orElse(null);

                public List<Object> syncedTeachers = item
                    .getTeacherItems()
                    .map(teacherItems ->
                        getSynchronizationItemLogEntries(teacherItems,
                            SynchronizationSummaryLog::getTeacherSynchronizationItemLogEntry))
                    .orElse(null);

                public List<MoodleUserEnrollments> moodleEnrollments = item.getMoodleEnrollments().orElse(null);
                public String message = item.getMessage();
            };
        } catch (Exception e) {
            LOGGER.error("Could not create log entry for synchronizationItem");
            e.printStackTrace();
            return null;
        }
    }

    private static <T extends EnrollmentSynchronizationItem> List<Object> getSynchronizationItemLogEntries(List<T> items,
                                                                 Function<T, Object> logEntryBuilder) {
        return items
            .stream()
            .map(item -> {
                try {
                    return logEntryBuilder.apply(item);
                } catch (Exception e) {
                    LOGGER.error("Could not create log entry for enrolmentSynchronizationItem");
                    e.printStackTrace();
                    return null;
                }
            })
            .collect(Collectors.toList());
    }

    private static Object getStudentSynchronizationItemLogEntry(StudentSynchronizationItem item) {
        return new SyncronizationItemLogEntry(item) {
            public String studentNumber = item.getStudent().studentNumber;
            public boolean approved = item.getStudent().approved;
        };
    }

    private static Object getTeacherSynchronizationItemLogEntry(TeacherSynchronizationItem item) {
        return new SyncronizationItemLogEntry(item) {
            public String teacherId = item.getTeacher().teacherId;
        };
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
