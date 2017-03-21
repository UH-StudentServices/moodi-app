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

package fi.helsinki.moodi.service.synchronize;

import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiCourseUsers;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;

public final class SynchronizationItem {

    public static final String SUCCESS_MESSAGE = "Success";
    public static final String ENROLLMENT_FAILURES_MESSAGE = "Some enrollment failures";

    private final Course course;
    private final SynchronizationType synchronizationType;
    private final boolean success;
    private final String message;
    private final Optional<OodiCourseUsers> oodiCourse;
    private final Optional<MoodleFullCourse> moodleCourse;
    private final Optional<List<MoodleUserEnrollments>> moodleEnrollments;
    private final List<UserSynchronizationItem> userSynchronizationItems;
    private final EnrichmentStatus enrichmentStatus;
    private final ProcessingStatus processingStatus;
    private final boolean unlock;
    private final boolean removed;

    public SynchronizationItem(Course course, SynchronizationType synchronizationType) {
        this(course, synchronizationType, false, "Started", empty(), empty(), empty(), newArrayList(), EnrichmentStatus.IN_PROGESS, ProcessingStatus.IN_PROGRESS, false, false);
    }

    private SynchronizationItem(
        Course course,
        SynchronizationType synchronizationType,
        boolean success,
        String message,
        Optional<OodiCourseUsers> oodiCourse,
        Optional<MoodleFullCourse> moodleCourse,
        Optional<List<MoodleUserEnrollments>> moodleEnrollments,
        List<UserSynchronizationItem> userSynchronizationItems,
        EnrichmentStatus enrichmentStatus,
        ProcessingStatus processingStatus,
        boolean unlock,
        boolean removed) {

        this.course = course;
        this.synchronizationType = synchronizationType;
        this.success = success;
        this.message = message;
        this.oodiCourse = oodiCourse;
        this.moodleCourse = moodleCourse;
        this.moodleEnrollments = moodleEnrollments;
        this.userSynchronizationItems = userSynchronizationItems;
        this.enrichmentStatus = enrichmentStatus;
        this.processingStatus = processingStatus;
        this.unlock = unlock;
        this.removed = removed;
    }

    public SynchronizationType getSynchronizationType() {
        return synchronizationType;
    }

    public Optional<OodiCourseUsers> getOodiCourse() {
        return oodiCourse;
    }

    public Optional<MoodleFullCourse> getMoodleCourse() {
        return moodleCourse;
    }

    public Optional<List<MoodleUserEnrollments>> getMoodleEnrollments() {
        return moodleEnrollments;
    }

    public SynchronizationItem setOodiCourse(final Optional<OodiCourseUsers> newOodiCourse) {
        return new SynchronizationItem(course, synchronizationType, success, message, newOodiCourse, moodleCourse, moodleEnrollments, userSynchronizationItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setMoodleCourse(final Optional<MoodleFullCourse> newMoodleCourse) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, newMoodleCourse, moodleEnrollments, userSynchronizationItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setMoodleEnrollments(final Optional<List<MoodleUserEnrollments>> newMoodleEnrollments) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, moodleCourse, newMoodleEnrollments, userSynchronizationItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setUserSynchronizationItems(final List<UserSynchronizationItem> newUserSynchronizationItems) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, moodleCourse, moodleEnrollments, newUserSynchronizationItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setUnlock(final boolean newUnlock) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, moodleCourse, moodleEnrollments, userSynchronizationItems, enrichmentStatus, processingStatus, newUnlock, removed);
    }

    public SynchronizationItem completeEnrichmentPhase(final EnrichmentStatus newEnrichmentStatus, final String newMessage) {
        final boolean newSuccess = newEnrichmentStatus == EnrichmentStatus.SUCCESS;
        return new SynchronizationItem(course, synchronizationType, newSuccess, newMessage, oodiCourse, moodleCourse, moodleEnrollments, userSynchronizationItems, newEnrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem completeProcessingPhase(final ProcessingStatus newProcessingStatus, final String newMessage) {
        return completeProcessingPhase(newProcessingStatus, newMessage, false);
    }

    public SynchronizationItem completeProcessingPhase(final ProcessingStatus newProcessingStatus, final String newMessage, final boolean newRemoved) {
        final boolean newSuccess = newProcessingStatus == ProcessingStatus.SUCCESS;
        return new SynchronizationItem(course, synchronizationType, newSuccess, newMessage, oodiCourse, moodleCourse, moodleEnrollments, userSynchronizationItems, enrichmentStatus, newProcessingStatus, unlock, newRemoved);
    }

    public Course getCourse() {
        return course;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public EnrichmentStatus getEnrichmentStatus() {
        return enrichmentStatus;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public boolean isUnlock() {
        return unlock;
    }

    public boolean isRemoved() {
        return removed;
    }

    public List<UserSynchronizationItem> getUserSynchronizationItems() {
        return userSynchronizationItems;
    }

    public SynchronizationItem completeProcessingPhase() {

        final boolean newSuccess = userSynchronizationItems.stream().allMatch(UserSynchronizationItem::isSuccess);

        final String newMessage = (newSuccess) ? SUCCESS_MESSAGE : ENROLLMENT_FAILURES_MESSAGE;

        final ProcessingStatus newProcessingStatus =
                (newSuccess) ? ProcessingStatus.SUCCESS : ProcessingStatus.ENROLLMENT_FAILURES;

        return completeProcessingPhase(newProcessingStatus, newMessage);
    }
}
