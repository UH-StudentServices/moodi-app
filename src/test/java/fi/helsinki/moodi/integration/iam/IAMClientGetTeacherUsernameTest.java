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

package fi.helsinki.moodi.integration.iam;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class IAMClientGetTeacherUsernameTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private IAMRestClient iamClient;

    @Test
    public void deserializeResponse() {
        iamMockServer.expect(
            requestTo("https://esbmt2.it.helsinki.fi/iam/findEmployee/19691981"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(Fixtures.asString("/iam/employee-username-19691981.json"),
                MediaType.APPLICATION_JSON));

        assertEquals(Arrays.asList("employee-username"), iamClient.getTeacherUsernameList("19691981"));
    }

    @Test
    public void thatEmptyTeacherResponseIsHandled() {
        iamMockServer.expect(
            requestTo("https://esbmt2.it.helsinki.fi/iam/findEmployee/007"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertNotNull(iamClient.getTeacherUsernameList("007"));
    }

}
