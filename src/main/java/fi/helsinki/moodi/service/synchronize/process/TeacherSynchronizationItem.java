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

import com.fasterxml.jackson.annotation.JsonView;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.util.JsonViews;

import java.util.Optional;

import static java.util.Optional.empty;

/**
 * Contains data needed to synchronize teacher enrollment.
 */
public final class TeacherSynchronizationItem implements EnrollmentSynchronizationItem {

    private final OodiTeacher teacher;
    private final long moodleRoleId;
    private final long moodleCourseId;
    private final boolean completed;
    private final boolean success;
    private final String message;
    private final EnrollmentSynchronizationStatus enrollmentSynchronizationStatus;

    private final Optional<String> username;
    private final Optional<MoodleUser> moodleUser;
    private final Optional<MoodleUserEnrollments> moodleEnrollments;

    public TeacherSynchronizationItem(OodiTeacher teacher, long moodleRoleId, long moodleCourse) {
        this(teacher, moodleRoleId, moodleCourse, false, false, "Started", EnrollmentSynchronizationStatus.STARTED, empty(), empty(), empty());
    }

    private TeacherSynchronizationItem(
            OodiTeacher teacher,
            long moodleRoleId,
            long moodleCourseId,
            boolean completed,
            boolean success,
            String message,
            EnrollmentSynchronizationStatus enrollmentSynchronizationStatus,
            Optional<String> username,
            Optional<MoodleUser> moodleUser,
            Optional<MoodleUserEnrollments> moodleEnrollments) {
        this.teacher = teacher;
        this.moodleRoleId = moodleRoleId;
        this.moodleCourseId = moodleCourseId;
        this.completed = completed;
        this.success = success;
        this.message = message;
        this.enrollmentSynchronizationStatus = enrollmentSynchronizationStatus;
        this.username = username;
        this.moodleUser = moodleUser;
        this.moodleEnrollments = moodleEnrollments;
    }

    public TeacherSynchronizationItem setUsername(Optional<String> newUsername) {
        return new TeacherSynchronizationItem(teacher, moodleRoleId, moodleCourseId, completed, success, message, enrollmentSynchronizationStatus, newUsername, moodleUser, moodleEnrollments);
    }

    public TeacherSynchronizationItem setMoodleUser(Optional<MoodleUser> newMoodleUser) {
        return new TeacherSynchronizationItem(teacher, moodleRoleId, moodleCourseId, completed, success, message, enrollmentSynchronizationStatus, username, newMoodleUser, moodleEnrollments);
    }

    public TeacherSynchronizationItem setMoodleEnrollments(Optional<MoodleUserEnrollments> newMoodleEnrollments) {
        return new TeacherSynchronizationItem(teacher, moodleRoleId, moodleCourseId, completed, success, message, enrollmentSynchronizationStatus, username, moodleUser, newMoodleEnrollments);
    }

    @JsonView(JsonViews.FileLogging.class)
    public OodiTeacher getTeacher() {
        return teacher;
    }

    @JsonView(JsonViews.FileLogging.class)
    public Optional<String> getUsername() {
        return username;
    }

    @JsonView(JsonViews.FileLogging.class)
    public Optional<MoodleUser> getMoodleUser() {
        return moodleUser;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getMoodleRoleId() {
        return moodleRoleId;
    }

    public long getMoodleCourseId() {
        return moodleCourseId;
    }

    public Optional<MoodleUserEnrollments> getMoodleEnrollments() {
        return moodleEnrollments;
    }

    @JsonView(JsonViews.FileLogging.class)
    public boolean isSuccess() {
        if (!completed) {
            throw new IllegalStateException("Not yet completed");
        }

        return success;
    }

    @JsonView(JsonViews.FileLogging.class)
    public String getMessage() {
        return message;
    }

    public TeacherSynchronizationItem setCompleted(boolean newSuccess, String newMessage, EnrollmentSynchronizationStatus newEnrollmentSynchronizationStatus) {
        if (isCompleted()) {
            throw new IllegalStateException("Already completed");
        }

        return new TeacherSynchronizationItem(teacher, moodleRoleId, moodleCourseId, true, newSuccess, newMessage, newEnrollmentSynchronizationStatus, username, moodleUser, moodleEnrollments);
    }

    @JsonView(JsonViews.FileLogging.class)
    public EnrollmentSynchronizationStatus getEnrollmentSynchronizationStatus() {
        return enrollmentSynchronizationStatus;
    }
}
