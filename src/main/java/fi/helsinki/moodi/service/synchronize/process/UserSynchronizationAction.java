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

package fi.helsinki.moodi.service.synchronize.process;

import java.util.List;

public class UserSynchronizationAction {

    public enum UserSynchronizationActionStatus {
        SUCCESS,
        ERROR
    }

    private final UserSynchronizationActionType actionType;
    private final List<Long> roles;
    private final Long moodleUserId;

    private UserSynchronizationActionStatus status;

    public UserSynchronizationAction(UserSynchronizationActionType actionType, List<Long> roles, Long moodleUserId) {
        this.actionType = actionType;
        this.roles = roles;
        this.moodleUserId = moodleUserId;
    }

    public UserSynchronizationActionType getActionType() {
        return actionType;
    }

    public List<Long> getRoles() {
        return roles;
    }

    public boolean isSuccess() {
        return UserSynchronizationActionStatus.SUCCESS.equals(this.status);
    }

    public Long getMoodleUserId() {
        return moodleUserId;
    }

    public UserSynchronizationActionStatus getStatus() {
        return status;
    }

    public UserSynchronizationAction withSuccessStatus() {
        return this.withStatus(UserSynchronizationActionStatus.SUCCESS);
    }

    public UserSynchronizationAction withErrorStatus() {
        return this.withStatus(UserSynchronizationActionStatus.ERROR);
    }

    private UserSynchronizationAction withStatus(UserSynchronizationActionStatus status) {
        this.status = status;
        return this;
    }

}
