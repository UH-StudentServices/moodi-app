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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import fi.helsinki.moodi.Application;
import fi.helsinki.moodi.integration.moodle.MoodleCourseData;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.synchronize.enrich.SisuCourseEnricher;
import fi.helsinki.moodi.service.util.MapperService;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    properties = { "server.port:0" },
    classes = { Application.class, TestConfig.class })

/*
  Many tests rely on there being exactly one course in the Moodi DB.
  Flyway does that: see test/resources/db/migration
 */
public abstract class AbstractMoodiIntegrationTest {
    protected static final long MOODLE_COURSE_ID = 988888L;
    protected static final long MOODLE_USER_ID_NIINA = 1L;
    protected static final long MOODLE_USER_ID_JUKKA = 2L;
    protected static final long MOODLE_USER_ID_MAKE = 3L;
    protected static final long MOODLE_USER_HRAOPE = 4L;
    protected static final long MOODLE_USER_TEACH_ONE = 5L;
    protected static final long MOODLE_USER_TEACH_TWO = 6L;
    protected static final long MOODLE_USER_TEACH_THREE = 7L;
    protected static final long MOODLE_USER_NIINA2 = 8L;
    protected static final long MOODLE_USER_NOT_ENROLLED = 9L;
    protected static final long MOODLE_USER_NOT_IN_STUDY_REGISTRY = 10L;
    protected static final long MOODLE_USER_CREATOR = 11L;

    protected static final String MOODLE_USERNAME_NIINA = "niina@helsinki.fi";
    protected static final String MOODLE_USERNAME_JUKKA = "jukka@helsinki.fi";
    protected static final String MOODLE_USERNAME_MAKE = "make@helsinki.fi";
    protected static final String MOODLE_USERNAME_HRAOPE = "hraopettaja@helsinki.fi";
    protected static final String MOODLE_USERNAME_ONE = "one@helsinki.fi";
    protected static final String MOODLE_USERNAME_TWO = "two@helsinki.fi";
    protected static final String MOODLE_USERNAME_THREE = "three@helsinki.fi";
    protected static final String MOODLE_USERNAME_NIINA2 = "niina2@helsinki.fi";
    protected static final String MOODLE_USERNAME_NOT_ENROLLED = "ei-mukana-kurssilla@helsinki.fi";
    protected static final String MOODLE_USERNAME_NOT_IN_STUDY_REGISTRY = "ei-sisussa@helsinki.fi";
    protected static final String MOODLE_USERNAME_CREATOR = "creator@helsinki.fi";

    protected static final String CREATOR_SISU_ID = "hy-hlo-creator";
    protected static final String PERSON_NOT_FOUND_ID = "hy-hlo-not-found";

    private static final String MOODLE_EMPTY_LIST_RESPONSE = "[]";
    protected static final String MOODLE_ERROR_RESPONSE =
        "{\"exception\":\"webservice_access_exception\",\"errorcode\":\"accessexception\",\"message\":\"P\\u00e4\\u00e4syn hallinnan poikkeus\"}";
    protected static final String MOODLE_NULL_OK_RESPONSE = "null";

    protected static final String OODI_ERROR_RESPONSE = "{\"exception\": {\"message\": \"Something went wrong\"}, \"status\": 500}";
    protected static final String OODI_NULL_DATA_RESPONSE = "{\"data\": null, \"status\": 200}";
    protected static final String OODI_EMPTY_RESPONSE = "{\"data\": { \"students\": [], \"teachers\": [] }, \"status\": 200}";

    protected static final List<String> SISU_COURSE_REALISATION_IDS = Arrays.asList("hy-CUR-1", "hy-CUR-2", "hy-CUR-ended", "hy-CUR-archived");
    private static final List<String> OODI_COURSE_REALISATION_IDS = Arrays.asList("111", "222", "333");
    private static final List<String> ALL_CUR_IDS = Stream.concat(SISU_COURSE_REALISATION_IDS.stream(), OODI_COURSE_REALISATION_IDS.stream())
        .distinct()
        .collect(Collectors.toList());

    protected static final String EMPTY_RESPONSE = "";

    protected static final int APPROVED_ENROLLMENT_STATUS_CODE = 3;
    protected static final int NON_APPROVED_ENROLLMENT_STATUS_CODE = 10;

    private static final ObjectMapper testObjectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected RestTemplate studyRegistryRestTemplate;

    @Autowired
    protected RestTemplate moodleRestTemplate;

    @Autowired
    protected RestTemplate moodleReadOnlyRestTemplate;

    @Autowired
    protected RestTemplate iamRestTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    protected Environment environment;

    @Autowired
    private MapperService mapperService;

    @Autowired
    protected SisuCourseEnricher sisuCourseEnricher;

    @Autowired
    private CacheManager cacheManager;

    protected MockMvc mockMvc;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, 9876);
    // Gets populated by the @Rule above.
    private MockServerClient mockServerClient;

    protected MockRestServiceServer studyRegistryMockServer;
    protected MockRestServiceServer moodleMockServer;
    protected MockRestServiceServer moodleReadOnlyMockServer;
    protected MockRestServiceServer iamMockServer;
    protected MockSisuGraphQLServer mockSisuGraphQLServer;

    protected String getMoodleBaseUrl() {
        return environment.getProperty("integration.moodle.baseUrl");
    }

    protected String getOodiUrl() {
        return environment.getProperty("integration.oodi.url");
    }

    protected String getSisuUrl() {
        return environment.getProperty("integration.sisu.baseUrl");
    }

    protected String getMoodleRestUrl() {
        return environment.getProperty("integration.moodle.baseUrl") + "/webservice/rest/server.php";
    }

    protected String getOodiCourseUnitRealisationRequestUrl(final String realisationId) {
        return String.format("%s/courseunitrealisations/%s?include_deleted=true&include_approved_status=true", getOodiUrl(), realisationId);
    }

    protected String getOodiCourseUsersRequestUrl(final String realisationId) {
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
                .build();
    }

    @Before
    public final void resetDBAndClearCache() {
        flyway.clean();
        flyway.migrate();
        cacheManager.getCacheNames().stream().forEach(c -> cacheManager.getCache(c).clear());
    }

    @Before
    public void setUpMockServers() {
        studyRegistryMockServer = MockRestServiceServer.createServer(studyRegistryRestTemplate);
        moodleMockServer = MockRestServiceServer.createServer(moodleRestTemplate);
        moodleReadOnlyMockServer = MockRestServiceServer.createServer(moodleReadOnlyRestTemplate);
        iamMockServer = MockRestServiceServer.createServer(iamRestTemplate);
        mockSisuGraphQLServer = new MockSisuGraphQLServer(mockServerClient);
    }

    @After
    public void verifyMockServers() {
        studyRegistryMockServer.verify();
        moodleMockServer.verify();
        moodleReadOnlyMockServer.verify();
        iamMockServer.verify();
        mockSisuGraphQLServer.verify();
    }

    public static String toJson(Object object) {
        try {
            return testObjectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void expectEnrollmentRequestToMoodle(final MoodleEnrollment... enrollments) {
        expectEnrollmentRequestToMoodleWithResponse(EMPTY_RESPONSE, enrollments);
    }

    protected final void expectSuspendRequestToMoodle(final MoodleEnrollment... enrollments) {
        expectSuspendRequestToMoodleWithResponse(EMPTY_RESPONSE, enrollments);
    }

    protected final void expectSuspendRequestToMoodleWithResponse(String response, final MoodleEnrollment... enrollments) {
        final String coreFunctionName = "enrol_manual_enrol_users";
        expectEnrollmentRequestToMoodleWithResponse(response, coreFunctionName, this::suspendUsersPartsBuilder, enrollments);
    }

    protected final void expectEnrollmentRequestToMoodleWithResponse(String response, final MoodleEnrollment... enrollments) {
        final String coreFunctionName = "enrol_manual_enrol_users";
        expectEnrollmentRequestToMoodleWithResponse(response, coreFunctionName, this::enrollUsersPartsBuilder, enrollments);
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

    protected final void expectAssignRolesToMoodle(boolean isAssign, MoodleEnrollment... enrollments) {
        expectAssignRolesToMoodleWithResponse(EMPTY_RESPONSE, isAssign, enrollments);
    }

    protected final void expectAssignRolesToMoodleWithResponse(String response, boolean isAssign, MoodleEnrollment... enrollments) {
        final String coreFunctionName = isAssign ? "core_role_assign_roles" : "core_role_unassign_roles";
        expectEnrollmentRequestToMoodleWithResponse(response, coreFunctionName, isAssign ? this::assignRolesPartsBuilder :
            this::unAssignRolesPartsBuilder, enrollments);
    }

    private Stream<String> enrollUsersPartsBuilder(MoodleEnrollment enrollment, int index) {
        final String property = "enrolments";

        return Stream.of(
            createEnrollmentRequestPart(property, "courseid", String.valueOf(enrollment.moodleCourseId), index),
            createEnrollmentRequestPart(property, "roleid", String.valueOf(enrollment.moodleRoleId), index),
            createEnrollmentRequestPart(property, "userid", String.valueOf(enrollment.moodleUserId), index));
    }

    private Stream<String> suspendUsersPartsBuilder(MoodleEnrollment enrollment, int index) {
        final String property = "enrolments";

        return Stream.of(
            createEnrollmentRequestPart(property, "courseid", String.valueOf(enrollment.moodleCourseId), index),
            createEnrollmentRequestPart(property, "roleid", String.valueOf(getMoodiRoleId()), index),
            createEnrollmentRequestPart(property, "userid", String.valueOf(enrollment.moodleUserId), index),
            createEnrollmentRequestPart(property, "suspend", "1", index)
        );
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

    private String createEnrollmentRequestPart(String property, String childProperty, String value, int index) {
        return property + "%5B" + index + "%5D%5B" + childProperty + "%5D=" + value;
    }

    protected final ResultActions makeCreateCourseRequest(final String realisationId) throws Exception {
        return makeCreateCourseRequest(realisationId, null);
    }

    protected final ResultActions makeCreateCourseRequest(final String realisationId, final String creatorSisuId) throws Exception {
        ImportCourseRequest importCourseRequest = new ImportCourseRequest();
        importCourseRequest.realisationId = realisationId;
        importCourseRequest.creatorSisuId = creatorSisuId;

        return mockMvc.perform(
            post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123")
                .content(toJson(importCourseRequest)));
    }

    protected final void expectFindStudentRequestToIAM(final String studentNumber, final String username) {
        final String response = "[{\"username\":\"" + username + "\",\"studentNumber\":\"" + studentNumber + "\"}]";
        expectFindStudentRequestToIAMWithResponse(studentNumber, response);
    }

    protected final void expectFindStudentRequestToIAMAndRespondWithEmptyResult(final String studentNumber) {
        final String response = "[]";
        expectFindStudentRequestToIAMWithResponse(studentNumber, response);
    }

    protected final void expectFindEmployeeRequestToIAM(final String employeeNumber, final String username) {
        final String response = "[{\"username\":\"" + username + "\",\"personnelNumber\":\"" + employeeNumber + "\"}]";
        iamMockServer.expect(requestTo("https://esbmt2.it.helsinki.fi/iam/findEmployee/" + employeeNumber))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private final void expectFindStudentRequestToIAMWithResponse(final String studentNumber, final String response) {
        iamMockServer.expect(requestTo("https://esbmt2.it.helsinki.fi/iam/findStudent/" + studentNumber))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected final void expectGetUserRequestToMoodleUserNotFound(final String username) {
        expectGetUserRequestToMoodleWithResponse(username, MOODLE_EMPTY_LIST_RESPONSE);
    }

    protected final void expectGetUserRequestToMoodle(final String username, final long userMoodleId) {
        expectGetUserRequestToMoodle(username, String.valueOf(userMoodleId));
    }

    protected final void expectGetUserRequestToMoodle(final String username, final String userMoodleId) {
        final String response = String.format("[{\"id\":\"%s\", \"username\":\"%s\", \"email\":\"\", \"fullname\":\"\"}]", userMoodleId, username);
        expectGetUserRequestToMoodleWithResponse(username, response);
    }

    private void expectGetUserRequestToMoodleWithResponse(String username, String response) {
        String payload = "wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json&field=username&values%5B0%5D="
            + urlEncode(username);
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(payload))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected final void expectGetCourseUsersRequestToOodi(final String realisationId, final ResponseCreator responseCreator) {
        final String url = getOodiCourseUsersRequestUrl(realisationId);
        studyRegistryMockServer.expect(requestTo(url))
            .andExpect(method(HttpMethod.GET))
            .andRespond(responseCreator);
    }

    protected final void expectCreateCourseRequestToMoodle(final String realisationId, final String moodleCourseIdPrefix,
                                                           final String description, final long moodleCourseIdToReturn,
                                                           final String categoryId) {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(
                allOf(
                    startsWith(
                        "wstoken=xxxx1234&wsfunction=core_course_create_courses&moodlewsrestformat=json&courses%5B0%5D%5Bidnumber%5D="
                            + moodleCourseIdPrefix + realisationId +
                            "&courses%5B0%5D%5Bfullname%5D=Lapsuus+ja+yhteiskunta&courses%5B0%5D%5Bshortname%5D=Lapsuus+ja"),
                    // The actual short name unique suffix is too difficult to check here, so we leave it out.
                    endsWith("&courses%5B0%5D%5Bcategoryid%5D=" + categoryId +
                        "&courses%5B0%5D%5Bsummary%5D=" + description + "&courses%5B0%5D%5Bvisible%5D=0" +
                        "&courses%5B0%5D%5Bstartdate%5D=1564952400&courses%5B0%5D%5Benddate%5D=1575496800" + // End date plus one month
                        "&courses%5B0%5D%5Bcourseformatoptions%5D%5B0%5D%5B" +
                        "name%5D=numsections&courses%5B0%5D%5Bcourseformatoptions%5D%5B0%5D%5Bvalue%5D=7"))))
            .andRespond(withSuccess("[{\"id\":\"" + moodleCourseIdToReturn + "\", \"shortname\":\"shortie\"}]", MediaType.APPLICATION_JSON));
    }

    protected void setupMoodleGetCourseResponse(int moodleId) {
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andRespond(withSuccess(Fixtures.asString("/moodle/get-courses-parameterized.json",
                new ImmutableMap.Builder()
                    .put("moodleId", moodleId)
                    .build()
                ), MediaType.APPLICATION_JSON));
    }

    protected void setupMoodleGetCourseResponse() {
        setupMoodleGetCourseResponse(54321);
    }

    protected final MoodleUserEnrollments getMoodleUserEnrollments(int moodleUserId, String userName, int enrolledCourseId, long...roleIds) {
        MoodleUserEnrollments ret = new MoodleUserEnrollments();
        ret.id = Long.valueOf(moodleUserId);
        ret.username = userName;
        ret.enrolledCourses = Arrays.asList(new MoodleCourseData(enrolledCourseId));
        ret.roles = Arrays.stream(roleIds).mapToObj(rid -> new MoodleRole(rid)).collect(Collectors.toList());
        return ret;
    }

    protected String getEnrollmentsResponse(int moodleUserId, String userName, int enrolledCourseId, long...roleIds) {
        long[] enrolledCourseIds = new long[] { enrolledCourseId };
        String ret = String.format(
            "[{ \"id\" : \"%s\", \"username\" : \"%s\", \"roles\" : [%s], \"enrolledcourses\" : [%s]}]",
            moodleUserId,
            userName,
            longArrayJson("roleid", roleIds),
            longArrayJson("id", enrolledCourseIds));

        return ret;
    }

    private String longArrayJson(String key, long[] ids) {
        return ids.length > 0 ?
            Arrays.stream(ids)
                .mapToObj(id -> String.format("{\"%s\" : %d}", key, id))
                .reduce((a, b) -> a.concat(",").concat(b))
                .get() :
            "";
    }

    protected final void expectGetEnrollmentsRequestToMoodle(final int courseId) {
        expectGetEnrollmentsRequestToMoodle(courseId, "[]");
    }

    protected final void expectGetEnrollmentsRequestToMoodle(final int courseId, MoodleUserEnrollments... responseEnrollments) {
        expectGetEnrollmentsRequestToMoodle(courseId, toJson(responseEnrollments));
    }

    protected final void expectGetEnrollmentsRequestToMoodle(final int courseId, final String response) {
        final String payload = "wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users&moodlewsrestformat=json&courseid=" + courseId;

        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(payload))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    protected void setUpMockSisuAndPrefetchCourses() {
        // With batch size 2 Moodi makes 2 calls to fetch Sisu CURs
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(SISU_COURSE_REALISATION_IDS.stream().limit(2).collect(Collectors.toList()),
            "/sisu/course-unit-realisations-1.json");
        mockSisuGraphQLServer.expectCourseUnitRealisationsRequest(SISU_COURSE_REALISATION_IDS.stream().skip(2).collect(Collectors.toList()),
            "/sisu/course-unit-realisations-2.json");

        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList("hy-hlo-1", "hy-hlo-2"), "/sisu/persons-many-1.json");
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList("hy-hlo-2.1", "hy-hlo-3"), "/sisu/persons-many-2.json");
        mockSisuGraphQLServer.expectPersonsRequest(Arrays.asList("hy-hlo-4"), "/sisu/persons.json");

        // Include Oodi course IDs, that should be ignored.
        sisuCourseEnricher.prefetchCourses(ALL_CUR_IDS);
    }

    protected void expectSisuOrganisationExportRequest() {
        studyRegistryMockServer.expect(requestTo(getSisuUrl() + "/kori/api/organisations/v2/export?limit=10000&since=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Fixtures.asString("/sisu/organisation-export.json"), MediaType.APPLICATION_JSON));
    }

    private String urlEncode(final String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
