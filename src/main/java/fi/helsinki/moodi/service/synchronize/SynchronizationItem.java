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
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;

/**
 * Contains data needed to synchronize single course.
 *
 * @see TeacherSynchronizationItem
 * @see StudentSynchronizationItem
 */
public final class SynchronizationItem {
    private final Course course;
    private final SynchronizationType synchronizationType;
    private final boolean success;
    private final String message;
    private final Optional<OodiCourseUnitRealisation> oodiCourse;
    private final Optional<MoodleFullCourse> moodleCourse;
    private final Optional<List<MoodleUserEnrollments>> moodleEnrollments;
    private final Optional<List<TeacherSynchronizationItem>> teacherItems;
    private final Optional<List<StudentSynchronizationItem>> studentItems;
    private final EnrichmentStatus enrichmentStatus;
    private final ProcessingStatus processingStatus;
    private final boolean unlock;
    private final boolean removed;

    public SynchronizationItem(Course course, SynchronizationType synchronizationType) {
        this(course, synchronizationType, false, "Started", empty(), empty(), empty(), empty(), empty(), EnrichmentStatus.IN_PROGESS, ProcessingStatus.IN_PROGRESS, false, false);
    }

    private SynchronizationItem(
            Course course,
            SynchronizationType synchronizationType,
            boolean success,
            String message,
            Optional<OodiCourseUnitRealisation> oodiCourse,
            Optional<MoodleFullCourse> moodleCourse,
            Optional<List<MoodleUserEnrollments>> moodleEnrollments,
            Optional<List<TeacherSynchronizationItem>> teacherItems,
            Optional<List<StudentSynchronizationItem>> studentItems,
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
        this.teacherItems = teacherItems;
        this.studentItems = studentItems;
        this.enrichmentStatus = enrichmentStatus;
        this.processingStatus = processingStatus;
        this.unlock = unlock;
        this.removed = removed;
    }

    public SynchronizationType getSynchronizationType() {
        return synchronizationType;
    }

    public Optional<OodiCourseUnitRealisation> getOodiCourse() {
        return oodiCourse;
    }

    public Optional<MoodleFullCourse> getMoodleCourse() {
        return moodleCourse;
    }

    public Optional<List<MoodleUserEnrollments>> getMoodleEnrollments() {
        return moodleEnrollments;
    }

    public SynchronizationItem setOodiCourse(final Optional<OodiCourseUnitRealisation> newOodiCourse) {
        return new SynchronizationItem(course, synchronizationType, success, message, newOodiCourse, moodleCourse, moodleEnrollments, teacherItems, studentItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setMoodleCourse(final Optional<MoodleFullCourse> newMoodleCourse) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, newMoodleCourse, moodleEnrollments, teacherItems, studentItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setMoodleEnrollments(final Optional<List<MoodleUserEnrollments>> newMoodleEnrollments) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, moodleCourse, newMoodleEnrollments, teacherItems, studentItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setTeacherItems(final Optional<List<TeacherSynchronizationItem>> newTeacherItems) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, moodleCourse, moodleEnrollments, newTeacherItems, studentItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setStudentItems(final Optional<List<StudentSynchronizationItem>> newStudentItems) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, moodleCourse, moodleEnrollments, teacherItems, newStudentItems, enrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem setUnlock(final boolean newUnlock) {
        return new SynchronizationItem(course, synchronizationType, success, message, oodiCourse, moodleCourse, moodleEnrollments, teacherItems, studentItems, enrichmentStatus, processingStatus, newUnlock, removed);
    }

    public SynchronizationItem completeEnrichmentPhase(final EnrichmentStatus newEnrichmentStatus, final String newMessage) {
        final boolean newSuccess = newEnrichmentStatus == EnrichmentStatus.SUCCESS;
        return new SynchronizationItem(course, synchronizationType, newSuccess, newMessage, oodiCourse, moodleCourse, moodleEnrollments, teacherItems, studentItems, newEnrichmentStatus, processingStatus, unlock, removed);
    }

    public SynchronizationItem completeProcessingPhase(final ProcessingStatus newProcessingStatus, final String newMessage) {
        return completeProcessingPhase(newProcessingStatus, newMessage, false);
    }

    public SynchronizationItem completeProcessingPhase(final ProcessingStatus newProcessingStatus, final String newMessage, final boolean newRemoved) {
        final boolean newSuccess = newProcessingStatus == ProcessingStatus.SUCCESS;
        return new SynchronizationItem(course, synchronizationType, newSuccess, newMessage, oodiCourse, moodleCourse, moodleEnrollments, teacherItems, studentItems, enrichmentStatus, newProcessingStatus, unlock, newRemoved);
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

    public Optional<List<TeacherSynchronizationItem>> getTeacherItems() {
        return teacherItems;
    }

    public Optional<List<StudentSynchronizationItem>> getStudentItems() {
        return studentItems;
    }

    public boolean isRemoved() {
        return removed;
    }

    public SynchronizationItem completeProcessingPhase() {
        if (!teacherItems.isPresent() || !studentItems.isPresent()) {
            throw new IllegalArgumentException("Student and teacher items need to be present");
        }

        final List<TeacherSynchronizationItem> processedTeacherItems = teacherItems.get();
        final List<StudentSynchronizationItem> processedStudentItems = studentItems.get();

        final long successfulTeacherItemsCount = processedTeacherItems.stream().filter(TeacherSynchronizationItem::isSuccess).count();
        final long successfulStudentItemsCount  = processedStudentItems.stream().filter(StudentSynchronizationItem::isSuccess).count();

        final boolean newSuccess = successfulStudentItemsCount == processedStudentItems.size() &&
                successfulTeacherItemsCount == processedTeacherItems.size();

        final String newMessage = (newSuccess) ? "Success" : "Some enrollment failures";
        final ProcessingStatus newProcessingStatus =
                (newSuccess) ? ProcessingStatus.SUCCESS : ProcessingStatus.ENROLLMENT_FAILURES;

        return completeProcessingPhase(newProcessingStatus, newMessage);
    }
}
