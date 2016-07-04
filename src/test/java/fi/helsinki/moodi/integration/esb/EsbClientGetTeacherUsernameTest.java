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

public class EsbClientGetTeacherUsernameTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private EsbClient esbClient;

    @Test
    public void deserializeResponse() {
        esbMockServer.expect(
            requestTo("https://esbmt1.it.helsinki.fi/iam/findEmployee/19691981"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(Fixtures.asString("/esb/employee-username-19691981.json"),
                MediaType.APPLICATION_JSON));

        assertEquals("arytkone", esbClient.getTeacherUsername("19691981"));
    }

    @Test
    public void thatEmptyTeacherResponseIsHandled() {
        esbMockServer.expect(
            requestTo("https://esbmt1.it.helsinki.fi/iam/findEmployee/007"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertNull(esbClient.getTeacherUsername("007"));
    }

}
