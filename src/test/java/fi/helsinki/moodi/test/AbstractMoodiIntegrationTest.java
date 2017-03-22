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
import fi.helsinki.moodi.Application;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.MoodleCourseBuilder;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.web.RequestLoggerFilter;
import org.flywaydb.core.Flyway;
import org.junit.After;
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
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { Application.class, TestConfig.class })
@WebIntegrationTest({
        "server.port:0"
})
@ActiveProfiles("test")
public abstract class AbstractMoodiIntegrationTest {
    private static final String EMPTY_LIST_RESPONSE = "[]";
    private static final String EXPECTED_COURSE_ID = MoodleCourseBuilder.MOODLE_COURSE_ID_PREFIX + 102374742;
    protected static final String ERROR_RESPONSE = "{\"exception\":\"webservice_access_exception\",\"errorcode\":\"accessexception\",\"message\":\"P\\u00e4\\u00e4syn hallinnan poikkeus\"}";
    protected static final String EMPTY_OK_RESPONSE = "";
    protected static final String NULL_OK_RESPONSE = "null";
    protected static final String NULL_DATA_RESPONSE = "{\"data\": null}";

    private static final ObjectMapper testObjectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected RestTemplate oodiRestTemplate;

    @Autowired
    protected RestTemplate moodleRestTemplate;

    @Autowired
    protected RestTemplate moodleReadOnlyRestTemplate;

    @Autowired
    protected RestTemplate esbRestTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    protected Environment environment;

    @Autowired
    private MapperService mapperService;

    protected MockMvc mockMvc;

    protected MockRestServiceServer oodiMockServer;
    protected MockRestServiceServer moodleMockServer;
    protected MockRestServiceServer moodleReadOnlyMockServer;
    protected MockRestServiceServer esbMockServer;

    protected String getMoodleBaseUrl() {
        return environment.getProperty("integration.moodle.baseUrl");
    }
    protected String getOodiUrl() {
        return environment.getProperty("integration.oodi.url");
    }

    protected String getMoodleRestUrl() {
        return environment.getProperty("integration.moodle.url");
    }

    protected String getOodiCourseUnitRealisationRequestUrl(final long realisationId) {
        return String.format("%s/courseunitrealisations/%s?include_deleted=true&include_approved_status=true", getOodiUrl(), realisationId);
    }

    protected String getOodiCourseUsersRequestUrl(final long realisationId) {
        return String.format("%s/courseunitrealisations/%s/users?include_deleted=true&include_approved_status=true", getOodiUrl(), realisationId);
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
    public void setUpMockServers() {
        oodiMockServer = MockRestServiceServer.createServer(oodiRestTemplate);
        moodleMockServer = MockRestServiceServer.createServer(moodleRestTemplate);
        moodleReadOnlyMockServer = MockRestServiceServer.createServer(moodleReadOnlyRestTemplate);
        esbMockServer = MockRestServiceServer.createServer(esbRestTemplate);
    }

    public static String toJson(Object object) throws IOException {
        return testObjectMapper.writeValueAsString(object);
    }

    protected final void expectEnrollmentRequestToMoodle(final MoodleEnrollment... enrollments) {
        expectEnrollmentRequestToMoodleWithResponse(EMPTY_OK_RESPONSE, enrollments);
    }

    protected final void expectEnrollmentRequestToMoodleWithResponse(String response, final MoodleEnrollment... enrollments) {
        final String coreFunctionName = "enrol_manual_enrol_users";
        expectEnrollmentRequestToMoodleWithResponse(response, coreFunctionName, this::enrollUsersPartsBuilder, enrollments);
    }

    protected final void expectAssignRolesToMoodle(boolean isAssign, MoodleEnrollment... enrollments) {
        expectAssignRolesToMoodleWithResponse(EMPTY_OK_RESPONSE, isAssign, enrollments);
    }

    protected final void expectAssignRolesToMoodleWithResponse(String response, boolean isAssign, MoodleEnrollment... enrollments) {
        final String coreFunctionName = isAssign ? "core_role_assign_roles" : "core_role_unassign_roles";
        expectEnrollmentRequestToMoodleWithResponse(response, coreFunctionName, isAssign ? this::assignRolesPartsBuilder : this::unAssignRolesPartsBuilder, enrollments);
    }

    private Stream<String> enrollUsersPartsBuilder(MoodleEnrollment enrollment, int index) {
        final String property = "enrolments";

        return Stream.of(
            createEnrollmentRequestPart(property, "courseid", String.valueOf(enrollment.moodleCourseId), index),
            createEnrollmentRequestPart(property, "roleid", String.valueOf(enrollment.moodleRoleId), index),
            createEnrollmentRequestPart(property, "userid", String.valueOf(enrollment.moodleUserId), index));
    }

    private Stream<String> updateRolesPartsBuilder(MoodleEnrollment enrollment, int index, String property) {
        return Stream.of(
            createEnrollmentRequestPart(property, "userid", String.valueOf(enrollment.moodleUserId), index),
            createEnrollmentRequestPart(property, "roleid", String.valueOf(enrollment.moodleRoleId), index),
            createEnrollmentRequestPart(property, "instanceid", String.valueOf(enrollment.moodleCourseId), index),
            createEnrollmentRequestPart(property, "contextlevel", "course", index));
    }

    private Stream<String> assignRolesPartsBuilder(MoodleEnrollment enrollment, int index) {
        return updateRolesPartsBuilder(enrollment, index, "assignments");
    }

    private Stream<String> unAssignRolesPartsBuilder(MoodleEnrollment enrollment, int index) {
       return updateRolesPartsBuilder(enrollment, index, "unassignments");
    }

    private void expectEnrollmentRequestToMoodleWithResponse(String response, String coreFunctionName,
                                                 BiFunction<MoodleEnrollment, Integer, Stream<String>> partsBuilder,
                                                 MoodleEnrollment... enrollments) {
        String partsSring = IntStream
            .range(0, enrollments.length)
            .mapToObj(index -> index)
            .flatMap(index -> partsBuilder.apply(enrollments[index], index))
            .collect(Collectors.joining("&"));

        final String payload = String.format("wstoken=xxxx1234&wsfunction=%s&moodlewsrestformat=json&%s", coreFunctionName, partsSring);

        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(payload))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private void expectEnrollmentRequestToMoodle(String coreFunctionName,
                                                 BiFunction<MoodleEnrollment, Integer, Stream<String>> partsBuilder,
                                                 MoodleEnrollment... enrollments) {
        expectEnrollmentRequestToMoodleWithResponse(EMPTY_OK_RESPONSE, coreFunctionName, partsBuilder, enrollments);
    }

    private String createEnrollmentRequestPart(String property, String childProperty, String value, int index) {
        return property + "%5B" + index + "%5D%5B" + childProperty + "%5D=" + value;
    }

    protected final ResultActions makeCreateCourseRequest(final long realisationId) throws Exception {
        return mockMvc.perform(
            post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123")
                .content(toJson(new ImportCourseRequest(realisationId))));
    }

    protected final void expectFindStudentRequestToEsb(final String studentNumber, final String username) {
        final String response = "[{\"username\":\"" + username + "\",\"studentNumber\":\"" + studentNumber + "\"}]";
        expectFindStudentRequestToEsbWithResponse(studentNumber, response);
    }

    protected final void expectFindStudentRequestToEsbAndRespondWithEmptyResult(final String studentNumber) {
        final String response = "[]";
        expectFindStudentRequestToEsbWithResponse(studentNumber, response);
    }

    protected final void expectFindEmployeeRequestToEsb(final String teacherId, final String username) {
        final String response = "[{\"username\":\"" + username + "\",\"personnelNumber\":\"" + teacherId + "\"}]";
        esbMockServer.expect(requestTo("https://esbmt1.it.helsinki.fi/iam/findEmployee/" + teacherId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private final void expectFindStudentRequestToEsbWithResponse(final String studentNumber, final String response) {
        esbMockServer.expect(requestTo("https://esbmt1.it.helsinki.fi/iam/findStudent/" + studentNumber))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected final void expectGetUserRequestToMoodleUserNotFound(final String username) {
        expectGetUserRequestToMoodleWithResponse(username, EMPTY_LIST_RESPONSE);
    }

    protected final void expectGetUserRequestToMoodle(final String username, final long userMoodleId) {
        expectGetUserRequestToMoodle(username, String.valueOf(userMoodleId));
    }

    protected final void expectGetUserRequestToMoodle(final String username, final String userMoodleId) {
        final String response = String.format("[{\"id\":\"%s\", \"username\":\"%s\", \"email\":\"\", \"fullname\":\"\"}]", userMoodleId, username);
        expectGetUserRequestToMoodleWithResponse(username, response);
    }

    private void expectGetUserRequestToMoodleWithResponse(String username, String response) {
        String payload = "wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json&field=username&values%5B0%5D=" + urlEncode(username);
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(payload))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected final void expectGetCourseUsersRequestToOodi(final long realisationId, final ResponseCreator responseCreator) {
        final String url = getOodiCourseUsersRequestUrl(realisationId);
        oodiMockServer.expect(requestTo(url))
            .andExpect(method(HttpMethod.GET))
            .andRespond(responseCreator);
    }

    protected final void expectGetCourseUnitRealisationRequestToOodi(final long realisationId, final ResponseCreator responseCreator) {
        final String url = getOodiCourseUnitRealisationRequestUrl(realisationId);
        oodiMockServer.expect(requestTo(url))
            .andExpect(method(HttpMethod.GET))
            .andRespond(responseCreator);
    }

    protected final void expectCreateCourseRequestToMoodle(final long realisationId, final long moodleCourseIdToReturn) {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(
                "wstoken=xxxx1234&wsfunction=core_course_create_courses&moodlewsrestformat=json&courses%5B0%5D%5Bidnumber%5D=" +
                    EXPECTED_COURSE_ID +
                    "&courses%5B0%5D%5Bfullname%5D=Lapsuus+ja+yhteiskunta&courses%5B0%5D%5Bshortname%5D=Lapsuus++" +
                    realisationId +
                    "&courses%5B0%5D%5Bcategoryid%5D=4&courses%5B0%5D%5Bsummary%5D=Description+1+%28fi%29+Description+2+%28fi%29&courses%5B0%5D%5Bformat%5D=topics&courses%5B0%5D%5Bmaxbytes%5D=20971520&courses%5B0%5D%5Bshowgrades%5D=1&courses%5B0%5D%5Bvisible%5D=0&courses%5B0%5D%5Bnewsitems%5D=5&courses%5B0%5D%5Bnumsections%5D=7&courses%5B0%5D%5Bshowreports%5D=0"))
            .andRespond(withSuccess("[{\"id\":\"" + moodleCourseIdToReturn + "\", \"shortname\":\"shortie\"}]", MediaType.APPLICATION_JSON));
    }

    protected final void expectGetEnrollmentsRequestToMoodle(final int courseId) {
        expectGetEnrollmentsRequestToMoodle(courseId, "[]");
    }

    protected final void expectGetEnrollmentsRequestToMoodle(final int courseId, final String response) {
        final String payload = "wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users&moodlewsrestformat=json&courseid=" + courseId;

        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(payload))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private String urlEncode(final String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void verify() {
        oodiMockServer.verify();
        moodleMockServer.verify();
        moodleReadOnlyMockServer.verify();
        esbMockServer.verify();
    }
}
