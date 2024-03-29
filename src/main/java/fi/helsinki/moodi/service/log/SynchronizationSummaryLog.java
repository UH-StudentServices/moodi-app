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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
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
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties("fullSummary")
public class SynchronizationSummaryLog {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizationSummaryLog.class);

    private final SynchronizationSummary fullSummary;

    public SynchronizationSummaryLog(SynchronizationSummary fullSummary) {
        this.fullSummary = fullSummary;
    }

    // as a public getter this gets called by json serializer, and this provides the summary as objects with
    // public fields, which are easily converted to json.
    public SynchronizationSummaryLogRoot getSyncronizationSummary() {
        return new SynchronizationSummaryLogRoot(
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
                    getFailedEnrollmentResults(userSyncronizationItemLogEntries)),
                item.getEnrichmentMessage(),
                item.getProcessingMessage()
            );
        } catch (Exception e) {
            logger.error("Could not create log entry for synchronizationItem", e);
            return null;
        }
    }

    private static Map<UserSynchronizationItemStatus, Long> getEnrollmentDetailsSummary(List<UserSyncronizationItemLogEntry> items) {
        return items.stream().collect(Collectors.groupingBy(item -> item.status, Collectors.counting()));
    }

    private static Map<UserSynchronizationItemStatus, List<UserSyncronizationItemLogEntry>> getFailedEnrollmentResults(
        List<UserSyncronizationItemLogEntry> items) {
        return items.stream().filter(i -> i.status != UserSynchronizationItemStatus.SUCCESS).collect(Collectors.groupingBy(item -> item.status));
    }

    public static class SynchronizationSummaryLogRoot {
        public final String elapsedTime;
        public final SynchronizationType type;
        public final long successfulItemsCount;
        public final long failedItemsCount;
        public final List<SynchronizationItemLogEntry> courses;

        public SynchronizationSummaryLogRoot(String elapsedTime,
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
        public final String realisationId;
        public final long moodleId;
        public final EnrichmentStatus enrichmentStatus;
        public final ProcessingStatus processingStatus;
        public final UserEnrollmentsLogEntry userEnrollments;
        public final String enrichmentMessage;
        public final String processingMessage;

        public SynchronizationItemLogEntry(String realisationId,
                                           long moodleId,
                                           EnrichmentStatus enrichmentStatus,
                                           ProcessingStatus processingStatus,
                                           UserEnrollmentsLogEntry userEnrollments,
                                           String enrichmentMessage,
                                           String processingMessage) {
            this.realisationId = realisationId;
            this.moodleId = moodleId;
            this.enrichmentStatus = enrichmentStatus;
            this.processingStatus = processingStatus;
            this.userEnrollments = userEnrollments;
            this.enrichmentMessage = enrichmentMessage;
            this.processingMessage = processingMessage;
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
        public final StudyRegistryStudent student;
        public final StudyRegistryTeacher teacher;

        public String moodleUsername;
        public List<Long> moodleRoleIds;

        private UserSyncronizationItemLogEntry(UserSynchronizationItem item) {
            this.status = item.getStatus();
            this.actions = item.getActions().stream().map(SyncronizationItemActionLogEntry::new).collect(Collectors.toList());
            this.moodleUserId = item.getMoodleUserId();
            this.student = item.getStudent();
            this.teacher = item.getTeacher();

            if (item.getMoodleUserEnrollments() != null) {
                this.moodleUsername = item.getMoodleUserEnrollments().username;
                this.moodleRoleIds = item.getMoodleUserEnrollments().roles
                    .stream()
                    .map(role -> role.roleId)
                    .collect(Collectors.toList());
            }
        }
    }

    public static class SyncronizationItemActionLogEntry {
        public final UserSynchronizationActionStatus status;
        public final UserSynchronizationActionType actionType;
        public final Set<Long> roles;

        public SyncronizationItemActionLogEntry(UserSynchronizationAction action) {
            this.status = action.getStatus();
            this.actionType = action.getActionType();
            this.roles = action.getRoles();
        }
    }
}
