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

public class IAMClientGetStudentUsernameTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private IAMClient iamClient;

    @Test
    public void deserializeResponseWithOneAccount() {
        iamMockServer.expect(
            requestTo("https://esbmt2.it.helsinki.fi/iam/findStudent/007"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(Fixtures.asString("/iam/student-username-007.json"), MediaType.APPLICATION_JSON));

        assertEquals(Arrays.asList("aunesluo"), iamClient.getStudentUserNameList("007"));
    }

    @Test
    public void deserializeResponseWithSeveralAccounts() {
        iamMockServer.expect(
                requestTo("https://esbmt2.it.helsinki.fi/iam/findStudent/008"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Fixtures.asString("/iam/student-username-008.json"), MediaType.APPLICATION_JSON));

        assertEquals(Arrays.asList("auneslu1", "aunesluo"), iamClient.getStudentUserNameList("008"));
    }

    @Test
    public void thatEmptyResponseIsHandled() {
        iamMockServer.expect(
            requestTo("https://esbmt2.it.helsinki.fi/iam/findStudent/009"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertNotNull(iamClient.getStudentUserNameList("009"));
    }

    @Test
    public void thatEmptyArrayIsHandled() {
        iamMockServer.expect(
                requestTo("https://esbmt2.it.helsinki.fi/iam/findStudent/0010"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertNotNull(iamClient.getStudentUserNameList("0010"));
    }

}
