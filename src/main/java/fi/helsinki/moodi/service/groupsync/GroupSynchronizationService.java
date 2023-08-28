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

package fi.helsinki.moodi.service.groupsync;

import fi.helsinki.moodi.exception.CourseImportNotReadyException;
import fi.helsinki.moodi.exception.CourseNotFoundException;
import fi.helsinki.moodi.exception.MoodleCourseNotFoundException;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuLocale;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseService;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class GroupSynchronizationService {
    private final CourseService courseService;
    private final MoodleService moodleService;
    private final SisuClient sisuClient;

    public SynchronizeGroupsResponse preview(@NotNull String realisationId) {
        final List<MoodleGroupingChange> changes = MoodleGroupChangeBuilder.buildChanges(createGroupSynchronizationContext(realisationId));
        return new SynchronizeGroupsResponse(changes);
    }

    public SynchronizeGroupsResponse process(@NotNull String realisationId) {
        val ctx = createGroupSynchronizationContext(realisationId);
        val changes = MoodleGroupChangeBuilder.buildChanges(ctx);
        MoodleGroupChangeProcessor moodleGroupChangeProcessor = new MoodleGroupChangeProcessor(moodleService, ctx);
        moodleGroupChangeProcessor.applyGroupingChanges(changes);
        return new SynchronizeGroupsResponse(changes);

    }

    private GroupSynchronizationContext createGroupSynchronizationContext(@NotNull String realisationId) {
        // 1. Get course unit realisation from Sisu
        final Optional<SisuCourseUnitRealisation> existingRealisation = sisuClient.getCourseUnitRealisation(realisationId);
        if (!existingRealisation.isPresent()) {
            throw new CourseNotFoundException(realisationId);
        }
        // 2. Get course mapping from DB
        final Optional<Course> existingCourse = courseService.findByRealisationId(realisationId);
        if (!existingCourse.isPresent()) {
            throw new CourseNotFoundException(realisationId);
        }

        if (existingCourse.get().importStatus != Course.ImportStatus.COMPLETED) {
            throw new CourseImportNotReadyException(existingCourse.get().id, existingCourse.get().importStatus.toString());
        }

        if (existingCourse.get().moodleId == null) {
            throw new MoodleCourseNotFoundException(existingCourse.get().id);
        }
        Course course = existingCourse.get();

        // 3. Get course from Moodle and related course data (groupings, groups, memberships)
        final MoodleFullCourse moodleCourse = moodleService.getCourse(course.moodleId)
            .orElseThrow(() -> new MoodleCourseNotFoundException(course.moodleId));

        String courseLanguage = moodleCourse.lang != null && !moodleCourse.lang.isEmpty()
            ? moodleCourse.lang
            : SisuLocale.byUrnOrDefaultToFi(existingRealisation.get().teachingLanguageUrn).toString();

        return GroupSynchronizationContext.builder()
            .sisuCourseUnitRealisation(existingRealisation.get())
            .course(course)
            .moodleCourse(moodleCourse)
            .courseLanguage(courseLanguage)
            .moodleGroupingsWithGroups(moodleService.getGroupingsWithGroups(moodleCourse.id, true))
            .moodleCourseMembers(moodleService.getEnrolledUsers(moodleCourse.id).stream()
            .map(MoodleUser::from).collect(java.util.stream.Collectors.toList()))
            .build();
    }
}
