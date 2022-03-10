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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.enrich.EnricherService;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichmentStatus;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class EnricherServiceTest extends AbstractMoodiIntegrationTest  {

    // add random 0-1000 millisecond delay to some moodle/sisu mock calls
    private boolean DELAYED = true;

    @Autowired
    private EnricherService enricherService;

    @Test
    public void thatEnrichersAreRanForEachItem() {
        List<SynchronizationItem> items = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            items.add(createFullSynchronizationItem("hy-CUR-" + i, i));
        }
        prepareSisuPrefetchMock(items);
        prepareMoodleMocks(items);
        // enrichers are run in this order and each item completes the whole sequence before taking the next item:
        // LockStatusEnricher 0
        // SisuCourseEnricher 1
        // MoodleCourseEnricher 2
        // MoodleEnrollmentsEnricher 3
        // CompletingEnricher 4
        List<SynchronizationItem> enrichedItems = enricherService.enrich(items);
        enrichedItems.forEach(item ->
            assertStatus(item, EnrichmentStatus.SUCCESS, true)
        );
    }

    private void assertStatus(SynchronizationItem item, EnrichmentStatus expectedStatus, boolean expectedCurPresent) {
        Optional<StudyRegistryCourseUnitRealisation> cur = item.getStudyRegistryCourse();

        assertEquals(expectedCurPresent, cur.isPresent());
        assertEquals(expectedStatus, item.getEnrichmentStatus());
    }

    private void prepareMoodleMocks(List<SynchronizationItem> items) {
        items.forEach(item -> {
            setupMoodleGetCourseResponse(item.getCourse().moodleId, DELAYED);
            expectGetEnrollmentsRequestToMoodle(item.getCourse().moodleId, Fixtures.asString("/moodle/get-enrolled-users.json"), DELAYED);
        });
    }

    private void prepareSisuPrefetchMock(List<SynchronizationItem> items) {
        List<String> curIds = items.stream().map(item -> item.getCourse().realisationId).collect(Collectors.toList());
        String curs = items.stream().map(item -> Fixtures.asString("/sisu/course-unit-realisation-template.json",
            new ImmutableMap.Builder<String, String>()
                .put("id", String.valueOf(item.getCourse().moodleId))
                .build())
        ).collect(Collectors.joining(", "));
        String response = "{\"data\": {\"course_unit_realisations\": [" + curs + "] } }";
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequestFromString(curIds, response, DELAYED);
        mockSisuGraphQLServer.expectPersonsRequest(singletonList("hy-hlo-1"), "/sisu/persons.json");
        sisuCourseEnricher.prefetchCourses(curIds);
    }

    private SynchronizationItem createFullSynchronizationItem(String realisationId, int moodleId) {
        Course course = new Course();
        course.realisationId = realisationId;
        course.moodleId = (long) moodleId;

        return new SynchronizationItem(course, SynchronizationType.FULL);
    }
}
