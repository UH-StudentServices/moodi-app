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

package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.integration.moodle.MoodleCourseWithEnrollments;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryService;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synclock.SyncLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class EnricherService {

    private final SyncLockService syncLockService;
    private final StudyRegistryService studyRegistryService;
    private final MoodleService moodleService;
    private Map<String, StudyRegistryCourseUnitRealisation> prefetchedCursById = new HashMap<>();
    private Map<Long, MoodleFullCourse> prefetchedMoodleCoursesById = new HashMap<>();
    private final Map<String, MoodleUser> prefetchedMoodleUsers = new HashMap<>();
    private final Map<Long, List<MoodleUserEnrollments>> prefetchedMoodleEnrollmentsByCourseId = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(EnricherService.class);

    @Autowired
    public EnricherService(SyncLockService syncLockService, StudyRegistryService studyRegistryService, MoodleService moodleService) {
        this.syncLockService = syncLockService;
        this.studyRegistryService = studyRegistryService;
        this.moodleService = moodleService;
    }

    /**
     * Enrich items with data required in synchronization.
     */
    public List<SynchronizationItem> enrichItems(final List<SynchronizationItem> items) {
        // First enrich all items with Sisu data
        prefetchSisuCourses(items.stream().map(item -> item.getCourse().realisationId).collect(toList()));
        items.forEach(this::enrichItemWithSisu);
        // Then enrich those items that are not locked or finished with Moodle data
        List<SynchronizationItem> activeItems = items.stream().filter(item -> !this.completed(item)).collect(toList());
        // Prefetching enrollments also gives us enrolled Moodle users, so we don't have to fetch them separately later
        prefetchMoodleCoursesEnrollmentsAndUsers(activeItems.stream().map(item -> item.getCourse().moodleId).collect(toList()));
        activeItems.forEach(this::enrichItemWithMoodle);
        return items;
    }

    public void enrichItemWithSisu(final SynchronizationItem item) {
        try {
            checkLockStatus(item);
            enrichWithSisuCourse(item);
        } catch (Exception e) {
            throw new EnrichException("Error enriching synchronization item", e);
        }
    }

    public void enrichItemWithMoodle(final SynchronizationItem item) {
        try {
            enrichWithMoodleCourse(item);
            enrichWithMoodleEnrollments(item);
            if (!completed(item)) {
                item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "Enrichment successful");
            }
        } catch (Exception e) {
            throw new EnrichException("Error enriching synchronization item", e);
        }
    }

    private boolean completed(final SynchronizationItem item) {
        if (item.getEnrichmentStatus() != EnrichmentStatus.IN_PROGRESS) {
            logger.debug("Item enrichment already completed, just return it");
            return true;
        }
        return false;
    }

    private void checkLockStatus(SynchronizationItem item) {
        if (completed(item)) {
            return;
        }
        try {
            final boolean isLocked = syncLockService.isLocked(item.getCourse());

            if (SynchronizationType.UNLOCK.equals(item.getSynchronizationType())) {
                item.setUnlock(true);
            } else if (isLocked) {
                item.completeEnrichmentPhase(EnrichmentStatus.LOCKED, "Item locked. Will not synchronize.");
            }
        } catch (Exception e) {
            logger.error("Error while enriching item (checkLockStatus)", e);
            item.completeEnrichmentPhase(EnrichmentStatus.ERROR, e.getMessage());
        }
    }

    public void prefetchSisuCourses(List<String> curIds) {
        List<String> uniqueSisuIds = new ArrayList<>(new LinkedHashSet<>(curIds));
        prefetchedCursById = studyRegistryService.getSisuCourseUnitRealisations(uniqueSisuIds).stream()
            .collect(Collectors.toMap(c -> c.realisationId, c -> c));
    }

    void enrichWithSisuCourse(SynchronizationItem item) {
        if (completed(item)) {
            return;
        }
        final Course course = item.getCourse();
        final StudyRegistryCourseUnitRealisation cur = prefetchedCursById.get(course.realisationId);

        if (cur == null) {
            item.completeEnrichmentPhase(
                EnrichmentStatus.ERROR,
                String.format("Course not found from Sisu with id %s", course.realisationId));
        } else if (endedMoreThanYearAgo(cur)) {
            item.completeEnrichmentPhase(
                EnrichmentStatus.COURSE_ENDED,
                String.format("Course with realisation id %s has ended", course.realisationId));
        } else {
            item.setStudyRegistryCourse(cur);
        }
    }

    private boolean endedMoreThanYearAgo(StudyRegistryCourseUnitRealisation cur) {
        return cur.endDate.plusYears(1).isBefore(LocalDate.now());
    }

    public void prefetchMoodleCoursesEnrollmentsAndUsers(List<Long> moodleCourseIds) {
        List<Long> uniqueMoodleCourseIds = new ArrayList<>(new LinkedHashSet<>(moodleCourseIds));
        prefetchedMoodleCoursesById = moodleService.getCourses(uniqueMoodleCourseIds).stream()
            .collect(Collectors.toMap(c -> c.id, c -> c));

        prefetchedMoodleUsers.clear();
        prefetchedMoodleEnrollmentsByCourseId.clear();
        List<MoodleCourseWithEnrollments> allEnrollments = moodleService.getEnrolledUsers(uniqueMoodleCourseIds);
        assert uniqueMoodleCourseIds.size() == allEnrollments.size() :
            "amount of courses with fetched enrollments (" + allEnrollments.size() + ") differs from amount of course ids: " +
            uniqueMoodleCourseIds.size();
        for (int i = 0; i < uniqueMoodleCourseIds.size(); i++) {
            long courseId = uniqueMoodleCourseIds.get(i);
            List<MoodleUserEnrollments> enrollments = allEnrollments.get(i).users;
            prefetchedMoodleEnrollmentsByCourseId.put(courseId, enrollments);
            enrollments.forEach(moodleEnrollment -> {
                if (!prefetchedMoodleUsers.containsKey(moodleEnrollment.username)) {
                    MoodleUser moodleUser = new MoodleUser();
                    moodleUser.id = moodleEnrollment.id;
                    prefetchedMoodleUsers.put(moodleEnrollment.username, moodleUser);
                }
            });
        }
    }

    public Optional<MoodleUser> getPrefetchedMoodleUser(List<String> usernameList) {
        for (String username: usernameList) {
            if (prefetchedMoodleUsers.containsKey(username)) {
                return Optional.of(prefetchedMoodleUsers.get(username));
            }
        }
        return Optional.empty();
    }

    private void enrichWithMoodleCourse(final SynchronizationItem item) {
        if (completed(item)) {
            return;
        }
        final Course course = item.getCourse();
        final MoodleFullCourse moodleCourse = prefetchedMoodleCoursesById.get(course.moodleId);

        if (moodleCourse == null) {
            item.completeEnrichmentPhase(
                EnrichmentStatus.MOODLE_COURSE_NOT_FOUND,
                "Course not found from Moodle with id " + course.moodleId);
        } else {
            item.setMoodleCourse(moodleCourse);
        }
    }

    private void enrichWithMoodleEnrollments(final SynchronizationItem item) {
        if (completed(item)) {
            return;
        }
        final Course course = item.getCourse();
        final List<MoodleUserEnrollments> moodleEnrollments = prefetchedMoodleEnrollmentsByCourseId.get(course.moodleId);
        item.setMoodleEnrollments(moodleEnrollments);
    }
}
