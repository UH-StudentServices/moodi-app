package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;

import java.util.Optional;

interface EnrollmentSynchronizationItem {

    Optional<String> getUsername();

    Optional<MoodleUser> getMoodleUser();

    Optional<MoodleUserEnrollments> getMoodleEnrollments();

    boolean isCompleted();

    boolean isSuccess();

    long getMoodleRoleId();

    long getMoodleCourseId();

    String getMessage();

    <T extends EnrollmentSynchronizationItem> T setCompleted(boolean newSuccess, String newMessage, EnrollmentSynchronizationStatus newEnrollmentSynchronizationStatus);

}
