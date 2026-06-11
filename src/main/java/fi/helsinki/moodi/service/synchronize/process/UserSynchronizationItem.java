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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import fi.helsinki.moodi.service.synchronize.snapshot.CourseSyncParticipant;
import org.apache.commons.lang3.StringUtils;

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
    private Long moodleCourseId;
    private MoodleUserEnrollments moodleUserEnrollments;

    private StudyRegistryStudent student;
    private StudyRegistryTeacher teacher;

    private CourseSyncParticipant existingSnapshot;

    private List<UserSynchronizationAction> actions = newArrayList();

    public UserSynchronizationItem(StudyRegistryStudent student) {
        this.student = student;
    }

    public UserSynchronizationItem(MoodleUserEnrollments moodleUserEnrollments) {
        this.moodleUserEnrollments = moodleUserEnrollments;
        this.moodleUser = new MoodleUser();
        this.moodleUser.setId(moodleUserEnrollments.id);
    }

    public UserSynchronizationItem(StudyRegistryTeacher teacher) {
        this.teacher = teacher;
    }

    public UserSynchronizationItem(CourseSyncParticipant existingSnapshot) {
        this.existingSnapshot = existingSnapshot;
        if (existingSnapshot.moodleUserId != null) {
            this.moodleUser = new MoodleUser();
            this.moodleUser.setId(existingSnapshot.moodleUserId);
        }
    }

    public static UserSynchronizationItem combine(UserSynchronizationItem firstItem, UserSynchronizationItem secondItem) {
        if (firstItem.getStudent() == null) {
            firstItem.setStudent(secondItem.getStudent());
        } else if (firstItem.getTeacher() == null) {
            firstItem.setTeacher(secondItem.getTeacher());
        }
        return firstItem;
    }

    public UserSynchronizationItem withMoodleUser(MoodleUser moodleUser) {
        this.moodleUser = moodleUser;
        return this;
    }

    public void setStudent(StudyRegistryStudent student) {
        this.student = student;
    }

    public void setTeacher(StudyRegistryTeacher teacher) {
        this.teacher = teacher;
    }

    public MoodleUser getMoodleUser() {
        return moodleUser;
    }

    public Long getMoodleUserId() {
        return moodleUser != null ? moodleUser.getId() : null;
    }

    public StudyRegistryStudent getStudent() {
        return student;
    }

    public StudyRegistryTeacher getTeacher() {
        return teacher;
    }

    public boolean isCompleted() {
        return !UserSynchronizationItemStatus.IN_PROGRESS.equals(status);
    }

    public boolean isSuccess() {
        return UserSynchronizationItemStatus.SUCCESS.equals(status);
    }

    public boolean isMoodleUserNotFound() {
        return UserSynchronizationItemStatus.MOODLE_USER_NOT_FOUND.equals(status);
    }

    public List<UserSynchronizationAction> getActions() {
        return actions;
    }

    public Long getMoodleCourseId() {
        return moodleCourseId;
    }

    public UserSynchronizationItem withMoodleCourseId(Long moodleCourseId) {
        this.moodleCourseId = moodleCourseId;
        return this;
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

    public boolean userSeesCourseInMoodle() {
        return moodleUserEnrollments != null && moodleUserEnrollments.seesCourse(this.moodleCourseId);
    }

    public CourseSyncParticipant getExistingSnapshot() {
        return existingSnapshot;
    }

    public UserSynchronizationItem withExistingSnapshot(CourseSyncParticipant existingSnapshot) {
        this.existingSnapshot = existingSnapshot;
        if (this.moodleUser == null && existingSnapshot != null && existingSnapshot.moodleUserId != null) {
            this.moodleUser = new MoodleUser();
            this.moodleUser.setId(existingSnapshot.moodleUserId);
        }
        return this;
    }

    public String getUsername() {
        if (student != null && StringUtils.isNotEmpty(student.userName)) {
            return student.userName;
        }
        if (teacher != null && StringUtils.isNotEmpty(teacher.userName)) {
            return teacher.userName;
        }
        if (existingSnapshot != null) {
            return existingSnapshot.username;
        }
        return moodleUserEnrollments != null ? moodleUserEnrollments.username : null;
    }

    /**
     * The snapshot row representing the current Sisu state of this user, to be persisted
     * after all actions for this user have been successfully applied to Moodle.
     */
    public CourseSyncParticipant toNewSnapshotState(Long courseId) {
        Long snapshotMoodleUserId = getMoodleUserId() != null ? getMoodleUserId() :
            existingSnapshot != null ? existingSnapshot.moodleUserId : null;
        return new CourseSyncParticipant(
            courseId,
            getUsername(),
            snapshotMoodleUserId,
            student != null,
            student != null && student.isEnrolled,
            teacher != null);
    }
}
