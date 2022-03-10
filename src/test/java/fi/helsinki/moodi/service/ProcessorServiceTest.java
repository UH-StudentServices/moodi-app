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

package fi.helsinki.moodi.service;

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.interceptor.AccessLoggingInterceptor;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import fi.helsinki.moodi.service.synchronize.process.ProcessorService;
import fi.helsinki.moodi.service.time.TimeService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class ProcessorServiceTest extends AbstractMoodiIntegrationTest {

    // add random 0-1000 millisecond delay to some moodle/sisu mock calls
    private boolean DELAYED = true;

    private static final String STUDENT_MOODLE_USERNAME = "studentUsername";
    private static final String TEACHER_MOODLE_USERNAME = "teacherUsername";
    private static final MoodleRole studentRole = new MoodleRole();
    private static final MoodleRole teacherRole = new MoodleRole();

    private static final long STUDENT_ROLE_ID = 5;
    private static final long TEACHER_ROLE_ID = 3;

    private final Logger logger = LoggerFactory.getLogger(ProcessorServiceTest.class);

    @Autowired
    private ProcessorService processorService;

    @Autowired
    private TimeService timeService;

    @MockBean
    private CourseRepository courseRepository;

    @Test
    public void thatProcessorsAreRanForEachItem() {
        studentRole.roleId = STUDENT_ROLE_ID;
        teacherRole.roleId = TEACHER_ROLE_ID;

        List<SynchronizationItem> items = new ArrayList<>();
        Map<Integer, ProcessingStatus> expected = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            items.add(createFullSynchronizationItem("hy-CUR-" + i, i));
        }
        SynchronizationItem item = items.get(0);
        items.set(0, item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "success"));
        when(courseRepository.findByRealisationId(item.getCourse().realisationId)).thenReturn(Optional.of(item.getCourse()));
        expected.put(0, ProcessingStatus.SUCCESS);

        items.set(1, items.get(1).completeEnrichmentPhase(EnrichmentStatus.IN_PROGRESS, "in progress"));
        expected.put(1, ProcessingStatus.SKIPPED);

        items.set(2, items.get(2).completeEnrichmentPhase(EnrichmentStatus.COURSE_ENDED, "course ended"));
        expected.put(2, ProcessingStatus.SUCCESS);
        items.set(3, items.get(3).completeEnrichmentPhase(EnrichmentStatus.COURSE_NOT_PUBLIC, "course not public"));
        expected.put(3, ProcessingStatus.SUCCESS);
        items.set(4, items.get(4).completeEnrichmentPhase(EnrichmentStatus.MOODLE_COURSE_NOT_FOUND, "moodle course not found"));
        expected.put(4, ProcessingStatus.SUCCESS);
        items.set(5, items.get(5).completeEnrichmentPhase(EnrichmentStatus.ERROR, "error"));
        expected.put(5, ProcessingStatus.SKIPPED);
        items.set(6, items.get(6).completeEnrichmentPhase(EnrichmentStatus.LOCKED, "locked"));
        expected.put(6, ProcessingStatus.SKIPPED);

        item = items.get(7);
        items.set(7, item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "success"));
        when(courseRepository.findByRealisationId(item.getCourse().realisationId)).thenReturn(Optional.of(item.getCourse()));
        expected.put(7, ProcessingStatus.SUCCESS);

        item = items.get(8);
        items.set(8, item.completeEnrichmentPhase(EnrichmentStatus.SUCCESS, "success"));
        when(courseRepository.findByRealisationId(item.getCourse().realisationId)).thenReturn(Optional.of(item.getCourse()));
        expected.put(8, ProcessingStatus.SUCCESS);

        // all items applicable for a certain action perform that action, then all items that are applicable for next action
        // SkippingProcessor Action.SKIP
        // RemovingProcessor Action.REMOVE
        // SynchronizingProcessor Action.SYNCHRONIZE
        List<SynchronizationItem> processedItems = processorService.process(items);
        // expecting first 3 in result list to be skipped, next 3 to be removed and last 3 processed
        int[] expectedOrder = {1, 5, 6, 2, 3, 4, 0, 7, 8};
        for (int i = 0; i < 9; i++) {
            item = processedItems.get(i);
            int id = item.getCourse().moodleId.intValue();
            assertEquals(expectedOrder[i], id);
            assertEquals(expected.get(id), item.getProcessingStatus());
        }
    }

    private SynchronizationItem createFullSynchronizationItem(String realisationId, int moodleId) {
        Course course = new Course();
        course.realisationId = realisationId;
        course.importStatus = Course.ImportStatus.IN_PROGRESS;
        course.moodleId = (long) moodleId;
        course.created = timeService.getCurrentUTCDateTime();
        course.modified = timeService.getCurrentUTCDateTime();
        course.removed = false;
        SynchronizationItem item = new SynchronizationItem(course, SynchronizationType.FULL);
        StudyRegistryCourseUnitRealisation cur = new StudyRegistryCourseUnitRealisation();
        cur.realisationId = realisationId;
        cur.realisationName = "course " + realisationId;
        item = item.setStudyRegistryCourse(Optional.of(cur));
        return item.setMoodleEnrollments(createExistingMoodleUserEnrollments());
    }

    private MoodleUserEnrollments createStudentEnrollment() {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.username = STUDENT_MOODLE_USERNAME;
        moodleUserEnrollments.roles = singletonList(studentRole);
        return moodleUserEnrollments;
    }

    private MoodleUserEnrollments createTeacherEnrollment() {
        MoodleUserEnrollments moodleUserEnrollments = new MoodleUserEnrollments();
        moodleUserEnrollments.username = TEACHER_MOODLE_USERNAME;
        moodleUserEnrollments.roles = singletonList(teacherRole);
        return moodleUserEnrollments;
    }

    private Optional<List<MoodleUserEnrollments>> createExistingMoodleUserEnrollments() {
        List<MoodleUserEnrollments> enrollments = new ArrayList<>();
        enrollments.add(createStudentEnrollment());
        enrollments.add(createTeacherEnrollment());
        return Optional.of(enrollments);
    }
}
