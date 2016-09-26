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

import java.util.List;
import java.util.Optional;

public interface EnrollmentSynchronizationItem {

    List<String> getUsernameList();

    Optional<MoodleUser> getMoodleUser();

    Optional<MoodleUserEnrollments> getMoodleEnrollments();

    boolean isCompleted();

    boolean isSuccess();

    long getMoodleRoleId();

    long getMoodleCourseId();

    String getMessage();

    <T extends EnrollmentSynchronizationItem> T setCompleted(boolean newSuccess, String newMessage, EnrollmentSynchronizationStatus newEnrollmentSynchronizationStatus);

    EnrollmentSynchronizationStatus getEnrollmentSynchronizationStatus();

}
