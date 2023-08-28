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

import fi.helsinki.moodi.Constants;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

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

    public Optional<MoodleFullCourse> getCourse(@NotNull Long courseId) {
        return this.getCourses(Collections.singletonList(courseId)).stream().findFirst();
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

    public List<MoodleGrouping> getGroupingsWithGroups(Long moodleCourseId, boolean includeMembers) {
        List<MoodleGroupingData> groupings = moodleClient.getCourseGroupings(moodleCourseId);
        List<Long> groupingIds = groupings.stream().map(MoodleGroupingData::getId).collect(Collectors.toList());
        // Only way to get groups for a grouping is to re-fetch them with flag to include groups
        final List<MoodleGrouping> groupingsWithGroups = moodleClient.getGroupings(groupingIds, true).stream()
            .map(MoodleGrouping::fromData).collect(Collectors.toList());
        if (includeMembers) {
            final List<MoodleGroup> groups = groupingsWithGroups.stream()
                .filter(g -> g.getGroups() != null)
                .flatMap(g -> g.getGroups().stream()).collect(Collectors.toList());
            final List<Long> groupIds = groups.stream()
                .map(MoodleGroup::getId).collect(Collectors.toList());
            final Map<Long, List<Long>> groupMembers = moodleClient.getGroupMembers(groupIds).stream()
                .collect(Collectors.toMap(MoodleGroupMembers::getGroupId, MoodleGroupMembers::getUserIds, (l1, l2) -> l1.addAll(l2) ? l1 : l2));
            if (!groupMembers.isEmpty()) {
                final List<MoodleUser> users = moodleClient.getUsers(groupMembers.values().stream()
                    .flatMap(List::stream).collect(Collectors.toList()));
                groups.forEach(g -> g.setMembers(
                    groupMembers.get(g.getId()).stream()
                        .map(userId -> users.stream()
                            .filter(u -> u.getId().equals(userId))
                            .findFirst()
                            .orElse(null)
                        )
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())));
            }
        }
        return groupingsWithGroups;
    }

    public Long createGrouping(MoodleGrouping grouping) {
        return moodleClient.createGrouping(new MoodleGroupingData(grouping));
    }

    public Long createGroup(MoodleGroup group, List<Long> groupingIds) {
        Long groupId = moodleClient.createGroup(new MoodleGroupData(group));
        assignGroupingsToGroup(groupingIds, groupId);
        return groupId;
    }

    public void addGroupMembers(Long groupId, List<Long> userIds) {
        moodleClient.addGroupMembers(groupId, userIds);
    }

    public void removeGroupMembers(Long groupId, List<Long> userIds) {
        moodleClient.deleteGroupMembers(groupId, userIds);
    }

    public MoodleGrouping getOrCreateSisuCommonGrouping(Long courseId, String courseLang) {
        val groupings = getGroupingsWithGroups(courseId, false);
        val sisuGrouping = groupings.stream()
            .filter(g -> Optional.ofNullable(g.getIdNumber()).orElse("").equals(Constants.MOODLE_SISU_COMMON_GROUPING_ID))
            .findFirst();
        if (sisuGrouping.isPresent()) {
            return sisuGrouping.get();
        } else {
            MoodleGrouping grouping = MoodleGrouping.newSisuCommonGrouping(
                courseId,
                courseLang
            );
            grouping.setId(moodleClient.createGrouping(new MoodleGroupingData(grouping)));
            return grouping;
        }
    }

    public void assignGroupingsToGroup(List<Long> groupingIds, Long groupId) {
        Map<Long, List<Long>> groupIdsByGroupingId = new HashMap<>();
        for (Long groupingId : groupingIds) {
            groupIdsByGroupingId.put(groupingId, Collections.singletonList(groupId));
        }
        if (!groupIdsByGroupingId.isEmpty()) {
            moodleClient.assignGroupings(groupIdsByGroupingId);
        }
    }

    public void deleteGroup(Long moodleGroupId) {
        moodleClient.deleteGroups(Collections.singletonList(moodleGroupId));
    }

    public void deleteGrouping(Long moodleGroupingId) {
        moodleClient.deleteGroupings(Collections.singletonList(moodleGroupingId));
    }
}
