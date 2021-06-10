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

package fi.helsinki.moodi.integration;

import com.google.common.base.Stopwatch;
import fi.helsinki.moodi.Application;
import fi.helsinki.moodi.integration.iam.IAMClient;
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import fi.helsinki.moodi.integration.oodi.OodiClient;
import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.test.TestConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    properties = {
        "integration.oodi.url=http://localhost:9876",
        "integration.moodle.baseUrl=http://localhost:9876",
        "integration.iam.url=http://localhost:9876",
        "integration.iam.apiKey=abloy",
        "integration.sisu.baseUrl=http://localhost:9876",
        "httpClient.connectTimeout=100",
        "httpClient.socketTimeout=100"
    },
    classes = { Application.class, TestConfig.class })
public class HttpClientTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, 9876);
    // Gets populated by the @Rule above.
    private MockServerClient mockServerClient;

    @Autowired
    private OodiClient oodiClient;

    @Autowired
    private SisuClient sisuClient;

    @Autowired
    private MoodleClient moodleClient;

    @Autowired
    private IAMClient iamClient;

    @Test
    public void thatOodiClientDoesNotHang() {
        testTimeout(this::callOodi);
    }

    @Test
    public void thatSisuClientDoesNotHang() {
        testTimeout(this::callSisu);
    }

    @Test
    public void thatMoodleClientDoesNotHang() {
        testTimeout(this::callMoodle);
    }

    @Test
    public void thatIAMClientDoesNotHang() {
        testTimeout(this::callIAM);
    }

    @Test
    public void thatIAMClientPassesApiKey() {
        mockServerClient.when(request().withHeader("Apikey", "abloy"))
                .respond(response().withBody("[]").withContentType(MediaType.JSON_UTF_8));
        iamClient.getStudentUserNameList("1");
    }

    private void testTimeout(Consumer<String> f) {
        takeLongToRespond(1);

        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            f.accept("1");
            fail("Wanted an exception but got none.");
        } catch (Exception e) {
            Throwable rootCause = Stream.iterate(e, Throwable::getCause)
                .filter(element -> element.getCause() == null)
                .findFirst().get();
            assertTrue(SocketTimeoutException.class.equals(rootCause.getClass()) || e.toString().contains("Read timed out"));
        }

        long took = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
        assertTrue(took >= 100 && took < 400);
    }

    private void callOodi(String id) {
        oodiClient.getCourseUnitRealisation(id);
    }

    private void callSisu(String id) {
        sisuClient.getCourseUnitRealisations(Arrays.asList(id));
    }

    private void callMoodle(String id) {
        moodleClient.getEnrolledUsers(1);
    }

    private void callIAM(String sn) {
        iamClient.getStudentUserNameList(sn);
    }

    public void takeLongToRespond(int seconds) {
        mockServerClient
            .when(request().withPath(".*"))
            .respond(response()
                .withBody("Am I late?")
                .withDelay(TimeUnit.SECONDS, seconds)
            );
    }

}
