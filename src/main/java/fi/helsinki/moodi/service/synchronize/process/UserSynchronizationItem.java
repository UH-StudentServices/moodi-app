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

import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class UserSynchronizationItem {

    public enum UserSynchronizationItemStatus {
        IN_PROGRESS,
        SUCCESS,
        USERNAME_NOT_FOUND,
        MOODLE_USER_NOT_FOUND,
        ERROR
    }

    private UserSynchronizationItemStatus status = UserSynchronizationItemStatus.IN_PROGRESS;

    private MoodleUser moodleUser;
    private MoodleUserEnrollments moodleUserEnrollments;

    private OodiStudent oodiStudent;
    private OodiTeacher oodiTeacher;

    private List<UserSynchronizationAction> actions = newArrayList();

    public UserSynchronizationItem(OodiStudent oodiStudent) {
        this.oodiStudent = oodiStudent;
    }

    public UserSynchronizationItem(OodiTeacher oodiTeacher) {
        this.oodiTeacher = oodiTeacher;
    }

    public static UserSynchronizationItem combine(UserSynchronizationItem firstItem, UserSynchronizationItem secondItem) {
        if (firstItem.getOodiStudent() == null) {
            firstItem.setOodiStudent(secondItem.getOodiStudent());
        } else if (firstItem.getOodiTeacher() == null) {
            firstItem.setOodiTeacher(secondItem.getOodiTeacher());
        }
        return firstItem;
    }

    public UserSynchronizationItem withMoodleUser(MoodleUser moodleUser) {
        this.moodleUser = moodleUser;
        return this;
    }

    public void setOodiStudent(OodiStudent oodiStudent) {
        this.oodiStudent = oodiStudent;
    }

    public void setOodiTeacher(OodiTeacher oodiTeacher) {
        this.oodiTeacher = oodiTeacher;
    }

    public MoodleUser getMoodleUser() {
        return moodleUser;
    }

    public Long getMoodleUserId() {
        return moodleUser != null ? moodleUser.id : null;
    }

    public OodiStudent getOodiStudent() {
        return oodiStudent;
    }

    public OodiTeacher getOodiTeacher() {
        return oodiTeacher;
    }

    public boolean isCompleted() {
        return !UserSynchronizationItemStatus.IN_PROGRESS.equals(status);
    }

    public boolean isSuccess() {
        return UserSynchronizationItemStatus.SUCCESS.equals(status);
    }

    public List<UserSynchronizationAction> getActions() {
        return actions;
    }

    public UserSynchronizationItem withActions(List<UserSynchronizationAction> actions) {
        this.actions = actions;
        return this;
    }

    public UserSynchronizationItem withMoodleUserEnrollments(MoodleUserEnrollments moodleUserEnrollments) {
        this.moodleUserEnrollments = moodleUserEnrollments;
        return this;
    }

    public MoodleUserEnrollments getMoodleUserEnrollments() {
        return this.moodleUserEnrollments;
    }

    public UserSynchronizationItemStatus getStatus() {
        return status;
    }

    public UserSynchronizationItem withStatus(UserSynchronizationItemStatus status) {
        this.status = status;
        return this;
    }
}
