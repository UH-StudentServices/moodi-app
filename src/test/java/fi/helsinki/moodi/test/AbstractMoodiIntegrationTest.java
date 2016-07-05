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

package fi.helsinki.moodi.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import fi.helsinki.moodi.Application;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.web.RequestLoggerFilter;
import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { Application.class, TestConfig.class })
@WebIntegrationTest({
        "server.port:0",
        "integration.moodle.wstoken:xxxx1234"
})
@ActiveProfiles("test")
public abstract class AbstractMoodiIntegrationTest {

    private static final ObjectMapper testObjectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RestTemplate oodiRestTemplate;

    @Autowired
    private RestTemplate moodleRestTemplate;

    @Autowired
    private RestTemplate esbRestTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private Environment environment;

    @Autowired
    private MapperService mapperService;

    protected MockMvc mockMvc;

    protected MockRestServiceServer oodiMockServer;
    protected MockRestServiceServer moodleMockServer;
    protected MockRestServiceServer esbMockServer;

    protected String getMoodleBaseUrl() {
        return environment.getProperty("integration.moodle.baseUrl");
    }

    protected String getMoodleRestUrl() {
        return environment.getProperty("integration.moodle.url");
    }

    protected long getStudentRoleId() {
        return mapperService.getStudentRoleId();
    }

    protected long getTeacherRoleId() {
        return mapperService.getTeacherRoleId();
    }

    protected long getMoodiRoleId() {
        return mapperService.getMoodiRoleId();
    }

    @Before
    public final void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new RequestLoggerFilter())
                .build();
    }

    @Before
    public final void executeMigrations() {
        flyway.clean();
        flyway.migrate();
    }

    @Before
    public final void setUpMockServers() {
        oodiMockServer = MockRestServiceServer.createServer(oodiRestTemplate);
        moodleMockServer = MockRestServiceServer.createServer(moodleRestTemplate);
        esbMockServer = MockRestServiceServer.createServer(esbRestTemplate);
    }

    public static String toJson(Object object) throws IOException {
        return testObjectMapper.writeValueAsString(object);
    }

    protected final void expectEnrollmentRequestToMoodle(final MoodleEnrollment... enrollments) {

        final List<String> parts = Lists.newArrayList();
        for (int i = 0; i < enrollments.length; i++) {
            final MoodleEnrollment enrollment = enrollments[i];
            final String part =
                "enrolments%5B" + i + "%5D%5Bcourseid%5D=" + enrollment.moodleCourseId +
                    "&enrolments%5B" + i + "%5D%5Broleid%5D=" + enrollment.moodleRoleId +
                    "&enrolments%5B" + i + "%5D%5Buserid%5D=" + enrollment.moodleUserId;
            parts.add(part);
        }

        final String enrollmentsPart = parts.stream()
            .collect(Collectors.joining("&"));

        final String payload = "wstoken=xxxx1234&wsfunction=enrol_manual_enrol_users&moodlewsrestformat=json" +
            (enrollmentsPart.isEmpty() ? "" : "&" + enrollmentsPart);

        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(payload))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
    }
}
