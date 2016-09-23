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

import java.util.Optional;

import static java.util.Optional.empty;

/**
 * Contains data needed to synchronize teacher enrollment.
 */
public final class StudentSynchronizationItem implements EnrollmentSynchronizationItem {

    private final OodiStudent student;
    private final long moodleRoleId;
    private final long moodleCourseId;
    private final boolean completed;
    private final boolean success;
    private final String message;
    private final EnrollmentSynchronizationStatus enrollmentSynchronizationStatus;

    private final Optional<String> username;
    private final Optional<MoodleUser> moodleUser;
    private final Optional<MoodleUserEnrollments> moodleEnrollments;

    public StudentSynchronizationItem(OodiStudent student, long moodleRoleId, long moodleCourse) {
        this(student, moodleRoleId, moodleCourse, false, false, "Started", EnrollmentSynchronizationStatus.STARTED, empty(), empty(), empty());
    }

    private StudentSynchronizationItem(
            OodiStudent student,
            long moodleRoleId,
            long moodleCourseId,
            boolean completed,
            boolean success,
            String message,
            EnrollmentSynchronizationStatus enrollmentSynchronizationStatus,
            Optional<String> username,
            Optional<MoodleUser> moodleUser,
            Optional<MoodleUserEnrollments> moodleEnrollments) {

        this.student = student;
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

    public StudentSynchronizationItem setUsername(Optional<String> newUsername) {
        return new StudentSynchronizationItem(student, moodleRoleId, moodleCourseId, completed, success, message, enrollmentSynchronizationStatus, newUsername, moodleUser, moodleEnrollments);
    }

    public StudentSynchronizationItem setMoodleUser(Optional<MoodleUser> newMoodleUser) {
        return new StudentSynchronizationItem(student, moodleRoleId, moodleCourseId, completed, success, message, enrollmentSynchronizationStatus, username, newMoodleUser, moodleEnrollments);
    }

    public StudentSynchronizationItem setMoodleEnrollments(Optional<MoodleUserEnrollments> newMoodleEnrollments) {
        return new StudentSynchronizationItem(student, moodleRoleId, moodleCourseId, completed, success, message, enrollmentSynchronizationStatus, username, moodleUser, newMoodleEnrollments);
    }

    public OodiStudent getStudent() {
        return student;
    }

    public Optional<String> getUsername() {
        return username;
    }

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

    public EnrollmentSynchronizationStatus getEnrollmentSynchronizationStatus() {
        return enrollmentSynchronizationStatus;
    }

    public Optional<MoodleUserEnrollments> getMoodleEnrollments() {
        return moodleEnrollments;
    }

    public boolean isSuccess() {
        if (!completed) {
            throw new IllegalStateException("Not yet completed");
        }

        return success;
    }

    public String getMessage() {
        return message;
    }

    public StudentSynchronizationItem setCompleted(boolean newSuccess, String newMessage, EnrollmentSynchronizationStatus newEnrollmentSynchronizationStatus) {
        if (isCompleted()) {
            throw new IllegalStateException("Already completed");
        }

        return new StudentSynchronizationItem(student, moodleRoleId, moodleCourseId, true, newSuccess, newMessage, newEnrollmentSynchronizationStatus, username, moodleUser, moodleEnrollments);
    }
}
