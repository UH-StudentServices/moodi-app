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

package fi.helsinki.moodi.integration.moodle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MoodleService {

    private final MoodleClient moodleClient;

    @Autowired
    public MoodleService(MoodleClient moodleClient) {
        this.moodleClient = moodleClient;
    }

    public long createCourse(final MoodleCourse course) {
        return moodleClient.createCourse(course);
    }

    public Optional<MoodleUser> getUser(final List<String> username) {
        if (username != null && !username.isEmpty()) {
            return Optional.ofNullable(moodleClient.getUser(username));
        } else {
            return Optional.empty();
        }
    }

    public long createUser(final String username, final String firstName, final String lastName,
                           final String email, final String password, final String idNumber) {
        return moodleClient.createUser(username, firstName, lastName, email, password, idNumber);
    }

    public void deleteUser(final long userId) {
        moodleClient.deleteUser(userId);
    }

    public List<MoodleFullCourse> getCourses(final List<Long> courseIds) {
        return moodleClient.getCourses(courseIds);
    }

    public List<MoodleUserEnrollments> getEnrolledUsers(final long courseId) {
        return moodleClient.getEnrolledUsers(courseId);
    }

    public void fetchEnrolledUsersForCourses(final Map<Long, List<MoodleUserEnrollments>> enrolmentsByCourseId, final List<Long> courseIds) {
        moodleClient.getEnrolledUsersForCourses(enrolmentsByCourseId, courseIds);
    }

    public void addRoles(final List<MoodleEnrollment> moodleEnrollments) {
        moodleClient.addRoles(moodleEnrollments);
    }

    public void removeRoles(final List<MoodleEnrollment> moodleEnrollments) {
        moodleClient.removeRoles(moodleEnrollments);
    }

    public void addEnrollments(final List<MoodleEnrollment> moodleEnrollments) {
        moodleClient.addEnrollments(moodleEnrollments);
    }

    public void suspendEnrollments(final List<MoodleEnrollment> moodleEnrollments) {
        moodleClient.suspendEnrollments(moodleEnrollments);
    }

    public Long updateCourseVisibility(final long courseId, boolean visible) {
        return moodleClient.updateCourseVisibility(courseId, visible);
    }
}
