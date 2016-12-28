package fi.helsinki.moodi.moodle;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.springframework.test.web.client.MockRestServiceServer;

public abstract class AbstractMoodleIntegrationTest extends AbstractMoodiIntegrationTest {

    @Before
    @Override
    public void setUpMockServers() {
        oodiMockServer = MockRestServiceServer.createServer(oodiRestTemplate);
        esbMockServer = MockRestServiceServer.createServer(esbRestTemplate);
    }

    @After
    @Override
    public void verify() {
        oodiMockServer.verify();
        esbMockServer.verify();
    }

}