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

package fi.helsinki.moodi.integration.oodi;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OodiClientGetCourseChangesTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private OodiClient oodiClient;

    @Test
    public void deserializeRespose() {
        oodiMockServer.expect(
            requestTo("https://esbmt2.it.helsinki.fi/doo-oodi/dev/testdb/courseunitrealisations/changes/ids/2015-01-20T00:00:00.000Z"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(Fixtures.asString("/oodi/course-changes.json"), MediaType.APPLICATION_JSON));

        final List<OodiCourseChange> changes = oodiClient.getCourseChanges(LocalDateTime.of(2015, 1, 20, 0, 0, 0));
        assertEquals(1, changes.size());
    }
}
