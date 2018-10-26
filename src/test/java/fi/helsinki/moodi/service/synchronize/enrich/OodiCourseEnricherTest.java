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
import fi.helsinki.moodi.integration.oodi.BaseOodiCourseUnitRealisation;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
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

    private static final String COURSE_NOT_FOUND_ERROR_MESSAGE = String.format("Course not found from Oodi with id %s", REALISATION_ID);
    private static final String EXCEPTION_MESSAGE = "Received exception with status 500 from from Oodi: Something went wrong";

    @Autowired
    private OodiCourseEnricher oodiCourseEnricher;

    private void setUpMockParameterizedResponseWithEndDate(String endDate) {
        expectGetCourseUsersRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                    "/oodi/parameterized-course-realisation.json",
                    new ImmutableMap.Builder()
                        .put("endDate", endDate)
                        .put("deleted", false)
                        .build()),
                MediaType.APPLICATION_JSON));
    }

    private void setUpMockResponse(String response) {
        expectGetCourseUsersRequestToOodi(
            REALISATION_ID,
            withSuccess(response,
                MediaType.APPLICATION_JSON));
    }

    private void setUpMockDeletedResponse() {
        expectGetCourseUsersRequestToOodi(
            REALISATION_ID,
            withSuccess(Fixtures.asString(
                    "/oodi/deleted-course-realisation.json"),
                MediaType.APPLICATION_JSON));
    }

    private SynchronizationItem createSynchronizationItem(long realisationId) {
        Course course = new Course();
        course.realisationId = realisationId;

        return new SynchronizationItem(course, SynchronizationType.FULL);
    }

    @Test
    public void thatOodiCourseIsFound() {
        String endDateInFuture = getFutureDateString();
        setUpMockParameterizedResponseWithEndDate(endDateInFuture);

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.doEnrich(synchronizationItem);

        Optional<BaseOodiCourseUnitRealisation> oodiCourseUsers = enrichedItem.getOodiCourse();

        assertTrue(oodiCourseUsers.isPresent());
        assertEquals(enrichedItem.getEnrichmentStatus(), EnrichmentStatus.IN_PROGESS);
    }

    @Test
    public void thatSynchronizationItemIsSetToErrorStatusWhenOodiResponseIsEmpty() {
        testErrorResponse(EMPTY_RESPONSE, COURSE_NOT_FOUND_ERROR_MESSAGE);
    }

    @Test
    public void thatSynchronizationItemIsSetToErrorStatusWhenOodiResponseDataIsNull() {
        testErrorResponse(OODI_NULL_DATA_RESPONSE, COURSE_NOT_FOUND_ERROR_MESSAGE);
    }

    @Test
    public void thatSynchronizationItemIsSetToErrorStatusWhenOodiRespondsWithError() {
        testErrorResponse(OODI_ERROR_RESPONSE, EXCEPTION_MESSAGE);
    }

    @Test
    public void thatSynchronizationItemIsSetToErrorStatusWhenOodiResponseDataContainsEmptyArraysForStudentsAndTeachers() {
        testErrorResponse(OODI_EMPTY_RESPONSE, COURSE_NOT_FOUND_ERROR_MESSAGE);
    }

    private void testErrorResponse(String response, String expectedMessage) {
        setUpMockResponse(response);

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.enrich(synchronizationItem);

        Optional<BaseOodiCourseUnitRealisation> oodiCourseUsers = enrichedItem.getOodiCourse();

        assertFalse(oodiCourseUsers.isPresent());
        assertEquals(EnrichmentStatus.ERROR, enrichedItem.getEnrichmentStatus());
        assertEquals(expectedMessage, enrichedItem.getEnrichmentMessage());
    }

    @Test
    public void thatOodiCourseIsEnded() {
        String endDateInPast = getOverYearAgoPastDateString();
        setUpMockParameterizedResponseWithEndDate(endDateInPast);

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.doEnrich(synchronizationItem);

        assertEquals(enrichedItem.getEnrichmentStatus(), EnrichmentStatus.OODI_COURSE_ENDED);
    }

    @Test
    public void thatOodiCourseIsRemoved() {
        setUpMockDeletedResponse();

        SynchronizationItem synchronizationItem = createSynchronizationItem(REALISATION_ID);

        SynchronizationItem enrichedItem = oodiCourseEnricher.doEnrich(synchronizationItem);

        assertEquals(enrichedItem.getEnrichmentStatus(), EnrichmentStatus.OODI_COURSE_REMOVED);
    }
}
