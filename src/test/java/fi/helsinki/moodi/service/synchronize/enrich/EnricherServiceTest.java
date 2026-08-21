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

import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.integration.moodle.MoodleCourseWithEnrollments;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

@TestPropertySource(properties = {"MoodleClient.batchsize=2", "SisuGraphQLClient.batchsize=2"})
public class EnricherServiceTest extends AbstractMoodiIntegrationTest  {

    // add random 0-1000 millisecond delay to some moodle/sisu mock calls
    private final boolean delayed = false;

    @Test
    public void thatSisuCoursesAndPersonsAreFetched() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createDeltaSynchronizationItem("hy-CUR-1"); // Published and ongoing

        enricherService.enrichWithSisuCourse(synchronizationItem);

        assertStatus(synchronizationItem, EnrichmentStatus.IN_PROGRESS, true);
    }

    @Test
    public void thatSynchronizationItemIsSetToErrorStatusWhenCourseIsNotFound() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createDeltaSynchronizationItem("hy-CUR-not-found");

        enricherService.enrichWithSisuCourse(synchronizationItem);

        assertStatus(synchronizationItem, EnrichmentStatus.ERROR, false);
    }

    @Test
    public void thatUnPublishedCourseGetsEnriched() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createDeltaSynchronizationItem("hy-CUR-unpublished");

        enricherService.enrichWithSisuCourse(synchronizationItem);

        assertStatus(synchronizationItem, EnrichmentStatus.IN_PROGRESS, true);
    }

    @Test
    public void thatSynchronizationItemIsSetToEndedStatusWhenCourseIsEnded() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createDeltaSynchronizationItem("hy-CUR-ended");

        enricherService.enrichWithSisuCourse(synchronizationItem);

        assertStatus(synchronizationItem, EnrichmentStatus.COURSE_ENDED, false);
    }

    private void assertStatus(SynchronizationItem item, EnrichmentStatus expectedStatus, boolean expectedCurPresent) {
        StudyRegistryCourseUnitRealisation cur = item.getStudyRegistryCourse();

        assertEquals(expectedCurPresent, cur != null);
        assertEquals(expectedStatus, item.getEnrichmentStatus());
    }

    private SynchronizationItem createDeltaSynchronizationItem(String realisationId) {
        Course course = new Course();
        course.realisationId = realisationId;

        return new SynchronizationItem(course, SynchronizationType.DELTA);
    }

    private SynchronizationItem createDeltaSynchronizationItem(String realisationId, int moodleId) {
        return createSynchronizationItem(realisationId, moodleId, SynchronizationType.DELTA);
    }

    private SynchronizationItem createSynchronizationItem(String realisationId, int moodleId, SynchronizationType type) {
        Course course = new Course();
        course.realisationId = realisationId;
        course.moodleId = (long) moodleId;

        return new SynchronizationItem(course, type);
    }

    @Test
    public void thatEnrichersAreRanForEachItem() {
        // DELTA items are enriched without fetching the Moodle enrollment state.
        List<SynchronizationItem> items = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            items.add(createDeltaSynchronizationItem("hy-CUR-" + i, i));
        }
        prepareSisuPrefetchMock(items);
        prepareMoodleGetCoursesResponseMock(
            items.stream().map(item -> item.getCourse().moodleId).toList(), delayed);

        List<SynchronizationItem> enrichedItems = enricherService.enrichItems(items);

        enrichedItems.forEach(item ->
            assertStatus(item, EnrichmentStatus.SUCCESS, true)
        );
    }

    @Test
    public void thatMoodleEnrollmentsAreFetchedForReconcileItems() {
        List<SynchronizationItem> items = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            items.add(createSynchronizationItem("hy-CUR-" + i, i, SynchronizationType.RECONCILE));
        }
        prepareSisuPrefetchMock(items);
        prepareMoodleGetCoursesResponseMock(
            items.stream().map(item -> item.getCourse().moodleId).toList(), delayed);
        prepareMoodleGetEnrolledUsersForCoursesMock(
            items.stream().map(item ->
                new MoodleCourseWithEnrollments(item.getCourse().moodleId, Collections.emptyList())).toList()
        );

        List<SynchronizationItem> enrichedItems = enricherService.enrichItems(items);

        enrichedItems.forEach(item ->
            assertStatus(item, EnrichmentStatus.SUCCESS, true)
        );
    }

    @Test
    public void thatMissingEnrollmentsCauseErrorForEnrichmentItem() {
        List<SynchronizationItem> items = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            items.add(createSynchronizationItem("hy-CUR-" + i, i, SynchronizationType.RECONCILE));
        }
        prepareSisuPrefetchMock(items);
        prepareMoodleGetCoursesResponseMock(
            items.stream().map(item -> item.getCourse().moodleId).toList(), delayed);
        prepareMoodleGetEnrolledUsersForCoursesMock(
            items.stream().map(item ->
                new MoodleCourseWithEnrollments(item.getCourse().moodleId, Collections.emptyList())).toList(),
            3);

        List<SynchronizationItem> enrichedItems = enricherService.enrichItems(items);

        // item 3 is set to fail
        for (int i = 0; i < enrichedItems.size(); i++) {
            assertStatus(enrichedItems.get(i), i == 3 ? EnrichmentStatus.ERROR : EnrichmentStatus.SUCCESS, true);
        }
    }

    @Test
    public void thatMixedRunFetchesEnrollmentsOnlyForReconcileItems() {
        List<SynchronizationItem> items = new ArrayList<>();
        items.add(createDeltaSynchronizationItem("hy-CUR-1", 1));
        items.add(createDeltaSynchronizationItem("hy-CUR-2", 2));
        items.add(createSynchronizationItem("hy-CUR-3", 3, SynchronizationType.RECONCILE));
        items.add(createSynchronizationItem("hy-CUR-4", 4, SynchronizationType.RECONCILE));

        prepareSisuPrefetchMock(items);
        prepareMoodleGetCoursesResponseMock(
            items.stream().map(item -> item.getCourse().moodleId).toList(), delayed);
        // The enrollment prefetch is narrowed to the RECONCILE courses only: a broader fetch
        // would generate unmatched requests and fail the mock server verification.
        prepareMoodleGetEnrolledUsersForCoursesMock(
            items.stream()
                .filter(item -> item.getSynchronizationType() == SynchronizationType.RECONCILE)
                .map(item -> new MoodleCourseWithEnrollments(item.getCourse().moodleId, Collections.emptyList())).toList()
        );

        List<SynchronizationItem> enrichedItems = enricherService.enrichItems(items);

        enrichedItems.forEach(item ->
            assertStatus(item, EnrichmentStatus.SUCCESS, true)
        );
    }

    @Test
    public void thatMissingEnrollmentsInMixedRunOnlyErrorReconcileItems() {
        List<SynchronizationItem> items = new ArrayList<>();
        items.add(createDeltaSynchronizationItem("hy-CUR-1", 1));
        items.add(createDeltaSynchronizationItem("hy-CUR-2", 2));
        items.add(createSynchronizationItem("hy-CUR-3", 3, SynchronizationType.RECONCILE));
        items.add(createSynchronizationItem("hy-CUR-4", 4, SynchronizationType.RECONCILE));

        prepareSisuPrefetchMock(items);
        prepareMoodleGetCoursesResponseMock(
            items.stream().map(item -> item.getCourse().moodleId).toList(), delayed);
        // The second reconcile course's enrollment fetch fails; the delta items never
        // consult the enrollment state and stay unaffected.
        prepareMoodleGetEnrolledUsersForCoursesMock(
            items.stream()
                .filter(item -> item.getSynchronizationType() == SynchronizationType.RECONCILE)
                .map(item -> new MoodleCourseWithEnrollments(item.getCourse().moodleId, Collections.emptyList())).toList(),
            1);

        List<SynchronizationItem> enrichedItems = enricherService.enrichItems(items);

        for (int i = 0; i < enrichedItems.size(); i++) {
            assertStatus(enrichedItems.get(i), i == 3 ? EnrichmentStatus.ERROR : EnrichmentStatus.SUCCESS, true);
        }
    }

    private void mockSisuCURRequestForBatch(List<SynchronizationItem> itemBatch) {
        String curs = itemBatch.stream().map(item -> Fixtures.asString("/sisu/course-unit-realisation-template.json",
            new ImmutableMap.Builder<String, String>()
                .put("id", String.valueOf(item.getCourse().moodleId))
                .build())
        ).collect(Collectors.joining(", "));
        String response = "{\"data\": {\"course_unit_realisations\": [" + curs + "] } }";
        List<String> curIds = itemBatch.stream().map(item -> item.getCourse().realisationId).toList();
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequestFromString(curIds, response, delayed);
    }

    private void prepareSisuPrefetchMock(List<SynchronizationItem> items) {
        int count = 1;
        int batchSize = 2;
        List<SynchronizationItem> itemBatch = new ArrayList<>();
        for (SynchronizationItem item: items) {
            itemBatch.add(item);
            if (count % batchSize == 0) {
                mockSisuCURRequestForBatch(itemBatch);
                itemBatch.clear();
            }
            count++;
        }
        if (!itemBatch.isEmpty()) {
            mockSisuCURRequestForBatch(itemBatch);
        }
        mockSisuGraphQLServer.expectPersonsRequest(singletonList("hy-hlo-1"), "/sisu/persons.json");
        List<String> curIds = items.stream().map(item -> item.getCourse().realisationId).toList();
        enricherService.prefetchSisuCourses(curIds);
    }
}
