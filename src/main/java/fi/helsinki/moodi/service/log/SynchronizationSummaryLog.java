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

package fi.helsinki.moodi.service.log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationAction;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationAction.UserSynchronizationActionStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationActionType;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem.UserSynchronizationItemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonIgnoreProperties("fullSummary")
public class SynchronizationSummaryLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationSummaryLog.class);

    private final SynchronizationSummary fullSummary;

    public SynchronizationSummaryLog(SynchronizationSummary fullSummary) {
        this.fullSummary = fullSummary;
    }

    public SynchronizationSymmaryLogRoot getSyncronizationSummary() {
        return new SynchronizationSymmaryLogRoot(
            fullSummary.getElapsedTime(),
            fullSummary.getType(),
            fullSummary.getSuccessfulItemsCount(),
            fullSummary.getFailedItemsCount(),
            fullSummary.getItems()
                .stream()
                .map(this::toSynchronizationItemLogEntry)
                .collect(Collectors.toList()));
    }

    private SynchronizationItemLogEntry toSynchronizationItemLogEntry(SynchronizationItem item) {
        try {
            List<UserSyncronizationItemLogEntry> userSyncronizationItemLogEntries = item.getUserSynchronizationItems().stream()
                .map(UserSyncronizationItemLogEntry::new)
                .collect(Collectors.toList());

            return new SynchronizationItemLogEntry(
                Optional.ofNullable(item.getCourse()).map(course -> course.realisationId).orElse(null),
                Optional.ofNullable(item.getCourse()).map(course -> course.moodleId).orElse(null),
                item.getEnrichmentStatus(),
                item.getProcessingStatus(),
                new UserEnrollmentsLogEntry(
                    getEnrollmentDetailsSummary(userSyncronizationItemLogEntries),
                    getEnrollmentResults(userSyncronizationItemLogEntries)),
                item.getMessage()
            );
        } catch (Exception e) {
            LOGGER.error("Could not create log entry for synchronizationItem", e);
            return null;
        }
    }

    private static Map<UserSynchronizationItemStatus, Long> getEnrollmentDetailsSummary(List<UserSyncronizationItemLogEntry> items) {
        return items.stream().collect(Collectors.groupingBy(item -> item.status, Collectors.counting()));
    }

    private static Map<UserSynchronizationItemStatus, List<UserSyncronizationItemLogEntry>> getEnrollmentResults(List<UserSyncronizationItemLogEntry> items) {
        return items.stream().collect(Collectors.groupingBy(item -> item.status));
    }

    public static class SynchronizationSymmaryLogRoot {
        public final String elapsedTime;
        public final SynchronizationType type;
        public final long successfulItemsCount;
        public final long failedItemsCount;
        public final List<SynchronizationItemLogEntry> courses;

        public SynchronizationSymmaryLogRoot(String elapsedTime,
                                             SynchronizationType type,
                                             long successfulItemsCount,
                                             long failedItemsCount,
                                             List<SynchronizationItemLogEntry> courses) {
            this.elapsedTime = elapsedTime;
            this.type = type;
            this.successfulItemsCount = successfulItemsCount;
            this.failedItemsCount = failedItemsCount;
            this.courses = courses;
        }
    }

    public static class SynchronizationItemLogEntry {
        public final long realisationId;
        public final long moodleId;
        public final EnrichmentStatus enrichmentStatus;
        public final ProcessingStatus processingStatus;
        public final UserEnrollmentsLogEntry userEnrollments;
        public final String message;

        public SynchronizationItemLogEntry(long realisationId,
                                           long moodleId,
                                           EnrichmentStatus enrichmentStatus,
                                           ProcessingStatus processingStatus,
                                           UserEnrollmentsLogEntry userEnrollments,
                                           String message) {
            this.realisationId = realisationId;
            this.moodleId = moodleId;
            this.enrichmentStatus = enrichmentStatus;
            this.processingStatus = processingStatus;
            this.userEnrollments = userEnrollments;
            this.message = message;
        }
    }

    public static class UserEnrollmentsLogEntry {
        public final Map<UserSynchronizationItemStatus, Long> summary;
        public final Map<UserSynchronizationItemStatus, List<UserSyncronizationItemLogEntry>> results;

        public UserEnrollmentsLogEntry(Map<UserSynchronizationItemStatus, Long> summary,
                                       Map<UserSynchronizationItemStatus, List<UserSyncronizationItemLogEntry>> results) {
            this.summary = summary;
            this.results = results;
        }
    }

    public static class UserSyncronizationItemLogEntry {
        public final UserSynchronizationItemStatus status;
        public final List<SyncronizationItemActionLogEntry> actions;
        public final Long moodleUserId;
        public final String studentNumber;
        public Boolean studentApproved;
        public final String teacherId;

        private UserSyncronizationItemLogEntry(UserSynchronizationItem item) {
            this.status = item.getStatus();
            this.actions = item.getActions().stream().map(SyncronizationItemActionLogEntry::new).collect(Collectors.toList());
            this.moodleUserId = item.getMoodleUserId();
            this.studentNumber = item.getOodiStudent() != null ? item.getOodiStudent().studentNumber : null;
            this.studentApproved = item.getOodiStudent() != null ? item.getOodiStudent().approved : null;
            this.teacherId = item.getOodiTeacher() != null ? item.getOodiTeacher().teacherId : null;
        }
    }

    public static class SyncronizationItemActionLogEntry {
        public final UserSynchronizationActionStatus status;
        public final UserSynchronizationActionType actionType;
        public final List<Long> roles;

        public SyncronizationItemActionLogEntry(UserSynchronizationAction action) {
            this.status = action.getStatus();
            this.actionType = action.getActionType();
            this.roles = action.getRoles();
        }
    }
}
