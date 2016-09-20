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
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Optional;

import static fi.helsinki.moodi.test.util.DateUtil.getFutureDateString;
import static fi.helsinki.moodi.test.util.DateUtil.getOverYearAgoPastDateString;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OodiCourseEnricherTest extends AbstractMoodiIntegrationTest {

    private static final long REALISATION_ID = 12345L;

    @Autowired
    private OodiCourseEnricher oodiCourseEnricher;

    private void setUpMockServerResponse(String endDate, boolean deleted) {
        expectGetCourseRealisationUnitRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                    "/oodi/parameterized-course-realisation.json",
                    new ImmutableMap.Builder()
                        .put("endDate", endDate)
                        .put("deleted", deleted)
                        .build()),
                MediaType.APPLICATION_JSON));
    }

    private void setUpMockServerEmptyResponse() {
        expectGetCourseRealisationUnitRequestToOodi(
            REALISATION_ID,
            withSuccess("{}",
                MediaType.APPLICATION_JSON));
    }

    private SynchronizationItem createSynchronizationItem(long realisationId) {
        Course course = new Course();
        course.realisationId = realisationId;

        return new SynchronizationItem(course);
    }

    @Test
    public void thatOodiCourseIsFound() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponse(endDateInFuture, false);

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.doEnrich(synchronizationItem);

        Optional<OodiCourseUnitRealisation> oodiCourseUnitRealisation = enrichedItem.getOodiCourse();

        assertTrue(oodiCourseUnitRealisation.isPresent());
        assertEquals(enrichedItem.getEnrichmentStatus(), EnrichmentStatus.IN_PROGESS);
    }

    @Test
    public void thatEnrichentIsSetToErrorStatusWhenEmptyResponse() {
        setUpMockServerEmptyResponse();

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.doEnrich(synchronizationItem);

        Optional<OodiCourseUnitRealisation> oodiCourseUnitRealisation = enrichedItem.getOodiCourse();

        assertFalse(oodiCourseUnitRealisation.isPresent());
        assertEquals(enrichedItem.getEnrichmentStatus(), EnrichmentStatus.ERROR);
    }

    @Test
    public void thatOodiCourseIsEnded() {
        String endDateInPast = getOverYearAgoPastDateString();
        setUpMockServerResponse(endDateInPast, false);

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.doEnrich(synchronizationItem);

        assertEquals(enrichedItem.getEnrichmentStatus(), EnrichmentStatus.OODI_COURSE_ENDED);
    }

    @Test
    public void thatOodiCourseIsRemoved() {
        String endDateInFuture = getFutureDateString();
        setUpMockServerResponse(endDateInFuture, true);

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.doEnrich(synchronizationItem);

        assertEquals(enrichedItem.getEnrichmentStatus(), EnrichmentStatus.OODI_COURSE_REMOVED);
    }
}
