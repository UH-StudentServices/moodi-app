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
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.test.TestConfig;
import org.junit.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(
    properties = {
        "integration.moodle.baseUrl=http://localhost:9876",
        "integration.sisu.baseUrl=http://localhost:9876",
        "httpClient.sisu.connectTimeout=100",
        "httpClient.sisu.socketTimeout=100",
        "httpClient.moodle.connectTimeout=100",
        "httpClient.moodle.socketTimeout=100",
        "spring.main.allow-bean-definition-overriding=true"
    },
    classes = { Application.class, TestConfig.class })
public class HttpClientTest {
    private WireMockServer wireMockServer;

    @org.junit.Before
    public void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9876));
        wireMockServer.start();
    }

    @org.junit.After
    public void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Autowired
    private SisuClient sisuClient;

    @Autowired
    private MoodleClient moodleClient;

    @Test
    public void thatSisuClientDoesNotHang() {
        testTimeout(this::callSisu);
    }

    @Test
    public void thatMoodleClientDoesNotHang() {
        testTimeout(this::callMoodle);
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
        assertTrue(took >= 100 && took < 900);
    }

    private void callSisu(String id) {
        sisuClient.getCourseUnitRealisations(Arrays.asList(id));
    }

    private void callMoodle(String id) {
        moodleClient.getEnrolledUsers(1);
    }

    public void takeLongToRespond(int seconds) {
        wireMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("Am I late?")
                        .withFixedDelay((int) TimeUnit.SECONDS.toMillis(seconds))
                )
        );
    }

}
