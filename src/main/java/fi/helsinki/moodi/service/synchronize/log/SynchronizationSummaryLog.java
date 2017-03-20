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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
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

                public Object users = getEnrollmentDetails(
                    item.getUserSynchronizationItems().stream().map(UserSyncronizationItemLogEntry::new)
                    .collect(Collectors.toList()));


                public String message = item.getMessage();
            };
        } catch (Exception e) {
            LOGGER.error("Could not create log entry for synchronizationItem", e);
            return null;
        }
    }

    private static Object getEnrollmentDetails(List<UserSyncronizationItemLogEntry> items) {
        if(items != null) {
            return new Object() {
                public Map<String, Long> summary = getEnrollmentDetailsSummary(items);
                public Map<String, List<UserSyncronizationItemLogEntry>> results = getEnrollmentResults(items);
            };
        } else {
            return null;
        }
    }

    private static Map<String, Long> getEnrollmentDetailsSummary(List<UserSyncronizationItemLogEntry> items) {
        return items.stream().collect(Collectors.groupingBy(item -> item.getStatus().name(), Collectors.counting()));
    }

    private static Map<String, List<UserSyncronizationItemLogEntry>> getEnrollmentResults(List<UserSyncronizationItemLogEntry> items) {
        return items.stream().collect(Collectors.groupingBy(item -> item.getStatus().name()));
    }

    private static class UserSyncronizationItemLogEntry {
        private final UserSynchronizationItemStatus status;
        private final List<SyncronizationItemActionLogEntry> actions;
        private final Long moodleUserId;

        private UserSyncronizationItemLogEntry(UserSynchronizationItem item) {
            this.status = item.getStatus();
            this.actions = item.getActions().stream().map(SyncronizationItemActionLogEntry::new).collect(Collectors.toList());
            this.moodleUserId = item.getMoodleUserId();
        }

        public List<SyncronizationItemActionLogEntry> getActions() {
            return actions;
        }

        public Long getMoodleUserId() {
            return moodleUserId;
        }

        public UserSynchronizationItemStatus getStatus() {
            return status;
        }
    }

    private static class SyncronizationItemActionLogEntry {
        private final UserSynchronizationActionStatus status;
        private final UserSynchronizationActionType actionType;
        private final List<Long> roles;

        public SyncronizationItemActionLogEntry(UserSynchronizationAction action) {
            this.status = action.getStatus();
            this.actionType = action.getActionType();
            this.roles = action.getRoles();
        }

        public UserSynchronizationActionStatus getStatus() {
            return status;
        }

        public UserSynchronizationActionType getActionType() {
            return actionType;
        }

        public List<Long> getRoles() {
            return roles;
        }
    }
}
