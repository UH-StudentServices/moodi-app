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

import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

@TestPropertySource(properties = "SisuGraphQLClient.batchsize=2")
public class SisuCourseEnricherTest extends AbstractMoodiIntegrationTest  {
    @Test
    public void thatSisuCoursesAndPersonsAreFetched() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createFullSynchronizationItem("hy-CUR-1"); // Published and ongoing

        SynchronizationItem enrichedItem = sisuCourseEnricher.doEnrich(synchronizationItem);

        assertStatus(enrichedItem, EnrichmentStatus.IN_PROGESS, true);
    }

    @Test
    public void thatSynchronizationItemIsSetToErrorStatusWhenCourseIsNotFound() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createFullSynchronizationItem("hy-CUR-not-found");

        SynchronizationItem enrichedItem = sisuCourseEnricher.doEnrich(synchronizationItem);

        assertStatus(enrichedItem, EnrichmentStatus.ERROR, false);
    }

    @Test
    public void thatSynchronizationItemIsSetToNotPublicStatusWhenCourseIsNotPublic() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createFullSynchronizationItem("hy-CUR-archived");

        SynchronizationItem enrichedItem = sisuCourseEnricher.doEnrich(synchronizationItem);

        assertStatus(enrichedItem, EnrichmentStatus.COURSE_NOT_PUBLIC, false);
    }

    @Test
    public void thatSynchronizationItemIsSetToNotEndedStatusWhenCourseIsEnded() {
        setUpMockSisuAndPrefetchCourses();
        SynchronizationItem synchronizationItem = createFullSynchronizationItem("hy-CUR-ended");

        SynchronizationItem enrichedItem = sisuCourseEnricher.doEnrich(synchronizationItem);

        assertStatus(enrichedItem, EnrichmentStatus.COURSE_ENDED, false);
    }

    private void assertStatus(SynchronizationItem item, EnrichmentStatus expectedStatus, boolean expectedCurPresent) {
        Optional<StudyRegistryCourseUnitRealisation> cur = item.getStudyRegistryCourse();

        assertEquals(expectedCurPresent, cur.isPresent());
        assertEquals(expectedStatus, item.getEnrichmentStatus());
    }

    private SynchronizationItem createFullSynchronizationItem(String realisationId) {
        Course course = new Course();
        course.realisationId = realisationId;

        return new SynchronizationItem(course, SynchronizationType.FULL);
    }
}
