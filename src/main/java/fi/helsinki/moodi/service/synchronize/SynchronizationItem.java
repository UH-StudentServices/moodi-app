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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class SynchronizationItem {

    public static final String SUCCESS_MESSAGE = "Success";
    public static final String ENROLLMENT_FAILURES_MESSAGE = "Some enrollment failures";

    private final Course course;
    private final SynchronizationType synchronizationType;
    private boolean success;

    private String enrichmentMessage;
    private String processingMessage;
    private StudyRegistryCourseUnitRealisation studyRegistryCourse;
    private MoodleFullCourse moodleCourse;
    private List<MoodleUserEnrollments> moodleEnrollments;
    private List<UserSynchronizationItem> userSynchronizationItems;
    private EnrichmentStatus enrichmentStatus;
    private ProcessingStatus processingStatus;
    private boolean unlock;
    private boolean removed;

    public SynchronizationItem(Course course, SynchronizationType synchronizationType) {
        this(course, synchronizationType, false, null, null, null, null, newArrayList(), newArrayList(), EnrichmentStatus.IN_PROGRESS,
            ProcessingStatus.IN_PROGRESS, false, false);
    }

    private SynchronizationItem(
        Course course,
        SynchronizationType synchronizationType,
        boolean success,
        String enrichmentMessage,
        String processingMessage,
        StudyRegistryCourseUnitRealisation studyRegistryCourse,
        MoodleFullCourse moodleCourse,
        List<MoodleUserEnrollments> moodleEnrollments,
        List<UserSynchronizationItem> userSynchronizationItems,
        EnrichmentStatus enrichmentStatus,
        ProcessingStatus processingStatus,
        boolean unlock,
        boolean removed) {

        this.course = course;
        this.synchronizationType = synchronizationType;
        this.success = success;
        this.enrichmentMessage = enrichmentMessage;
        this.processingMessage = processingMessage;
        this.studyRegistryCourse = studyRegistryCourse;
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

    public StudyRegistryCourseUnitRealisation getStudyRegistryCourse() {
        return studyRegistryCourse;
    }

    public void setStudyRegistryCourse(StudyRegistryCourseUnitRealisation studyRegistryCourse) {
        this.studyRegistryCourse = studyRegistryCourse;
    }

    public MoodleFullCourse getMoodleCourse() {
        return moodleCourse;
    }

    public void setMoodleCourse(MoodleFullCourse moodleCourse) {
        this.moodleCourse = moodleCourse;
    }

    public List<MoodleUserEnrollments> getMoodleEnrollments() {
        return moodleEnrollments;
    }

    public void setMoodleEnrollments(List<MoodleUserEnrollments> moodleEnrollments) {
        this.moodleEnrollments = moodleEnrollments;
    }

    public void setUnlock(final boolean unlock) {
        this.unlock = unlock;
    }

    public void completeEnrichmentPhase(EnrichmentStatus enrichmentStatus, String enrichmentMessage) {
        this.success = enrichmentStatus == EnrichmentStatus.SUCCESS;
        this.enrichmentMessage = enrichmentMessage;
        this.enrichmentStatus = enrichmentStatus;
    }

    public void completeProcessingPhase(ProcessingStatus processingStatus, String processingMessage) {
        completeProcessingPhase(processingStatus, processingMessage, false);
    }

    public void completeProcessingPhase(ProcessingStatus processingStatus, String processingMessage,
                                                        boolean removed) {
        this.success = processingStatus == ProcessingStatus.SUCCESS;
        this.processingMessage = processingMessage;
        this.processingStatus = processingStatus;
        this.removed = removed;
    }

    public void completeProcessingPhase() {
        boolean newSuccess = userSynchronizationItems.stream().allMatch(i -> i.isSuccess() || i.isMoodleUserNotFound());

        final String newProcessingMessage = (newSuccess) ? SUCCESS_MESSAGE : ENROLLMENT_FAILURES_MESSAGE;

        final ProcessingStatus newProcessingStatus =
            (newSuccess) ? ProcessingStatus.SUCCESS : ProcessingStatus.ENROLLMENT_FAILURES;

        completeProcessingPhase(newProcessingStatus, newProcessingMessage);
    }

    public Course getCourse() {
        return course;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getEnrichmentMessage() {
        return enrichmentMessage;
    }

    public String getProcessingMessage() {
        return processingMessage;
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

    public void setUserSynchronizationItems(final List<UserSynchronizationItem> userSynchronizationItems) {
        this.userSynchronizationItems = userSynchronizationItems;
    }
}
