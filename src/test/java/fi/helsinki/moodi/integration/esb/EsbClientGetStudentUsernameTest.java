package fi.helsinki.moodi.integration.esb;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class EsbClientGetStudentUsernameTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private EsbClient esbClient;

    @Test
    public void deserializeResponse() {
        esbMockServer.expect(
            requestTo("https://esbmt1.it.helsinki.fi/iam/findStudent/007"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(Fixtures.asString("/esb/student-username-007.json"), MediaType.APPLICATION_JSON));

        assertEquals("aunesluo", esbClient.getStudentUsername("007"));
    }

    @Test
    public void thatEmptyResponseIsHandled() {
        esbMockServer.expect(
            requestTo("https://esbmt1.it.helsinki.fi/iam/findStudent/007"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertNull(esbClient.getStudentUsername("007"));
    }

}
