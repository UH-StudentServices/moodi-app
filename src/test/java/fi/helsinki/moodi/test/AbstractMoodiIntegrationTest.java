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
import com.fasterxml.jackson.databind.ObjectWriter;
import fi.helsinki.moodi.Application;
import fi.helsinki.moodi.integration.moodle.MoodleCourseData;
import fi.helsinki.moodi.integration.moodle.MoodleCourseWithEnrollments;
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleRole;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.synchronize.enrich.EnricherService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    properties = { "server.port:0", "spring.main.allow-bean-definition-overriding=true" },
    classes = { Application.class, TestConfig.class })

/*
  Many tests rely on there being exactly one course in the Moodi DB.
  Flyway does that: see test/resources/db/migration
 */
public abstract class AbstractMoodiIntegrationTest {
    protected static final String SISU_REALISATION_IN_DB_ID = "hy-cur-in-db";
    protected static final String SISU_REALISATION_NOT_IN_DB_ID = "hy-cur-not-in-db";
    protected static final long MOODLE_COURSE_ID_IN_DB = 54321L;
    protected static final long MOODLE_COURSE_ID_NOT_IN_DB = 988888L;
    protected static final long SOME_OTHER_MOODLE_COURSE_ID = 999L;
    protected static final long MOODLE_USER_ID_NIINA = 1L;
    protected static final long MOODLE_USER_ID_JUKKA = 2L;
    protected static final long MOODLE_USER_ID_MAKE = 3L;
    protected static final long MOODLE_USER_HRAOPE = 4L;
    protected static final long MOODLE_USER_TEACH_ONE = 5L;
    protected static final long MOODLE_USER_TEACH_TWO = 6L;
    protected static final long MOODLE_USER_TEACH_THREE = 7L;
    protected static final long MOODLE_USER_NIINA2 = 8L;
    protected static final long MOODLE_USER_NOT_ENROLLED_IN_SISU = 9L;
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
    protected static final String MOODLE_USERNAME_NOT_ENROLLED_IN_SISU = "ei-mukana-kurssilla@helsinki.fi";
    protected static final String MOODLE_USERNAME_NOT_IN_STUDY_REGISTRY = "ei-sisussa@helsinki.fi";
    protected static final String MOODLE_USERNAME_CREATOR = "creator@helsinki.fi";

    protected static final String CREATOR_SISU_ID = "hy-hlo-creator";
    protected static final String PERSON_NOT_FOUND_ID = "hy-hlo-not-found";

    private static final String MOODLE_EMPTY_LIST_RESPONSE = "[]";
    protected static final String MOODLE_ERROR_RESPONSE =
        "{\"exception\":\"webservice_access_exception\",\"errorcode\":\"accessexception\",\"message\":\"P\\u00e4\\u00e4syn hallinnan poikkeus\"}";
    protected static final String MOODLE_NULL_OK_RESPONSE = "null";

    protected static final List<String> SISU_COURSE_REALISATION_IDS = Arrays.asList("hy-CUR-1", "hy-CUR-2", "hy-CUR-ended", "hy-CUR-archived");

    protected static final String EMPTY_RESPONSE = "";

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
    private Flyway flyway;

    @Autowired
    protected Environment environment;

    @Autowired
    private MapperService mapperService;

    @Autowired
    protected EnricherService enricherService;

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
    protected MockSisuGraphQLServer mockSisuGraphQLServer;

    protected String getMoodleBaseUrl() {
        return environment.getProperty("integration.moodle.baseUrl");
    }

    protected String getSisuUrl() {
        return environment.getProperty("integration.sisu.baseUrl");
    }

    protected String getMoodleRestUrl() {
        return environment.getProperty("integration.moodle.baseUrl") + "/webservice/rest/server.php";
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
        mockSisuGraphQLServer = new MockSisuGraphQLServer(mockServerClient);
    }

    @After
    public void verifyMockServers() {
        studyRegistryMockServer.verify();
        moodleReadOnlyMockServer.verify();
        moodleMockServer.verify();
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
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
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

    protected final void expectGetUserRequestToMoodleUserNotFound(final String username) {
        expectGetUserRequestToMoodleWithResponse(username, MOODLE_EMPTY_LIST_RESPONSE, false);
    }

    protected final void expectGetUserRequestToMoodle(final String username, final long userMoodleId, boolean delayed) {
        expectGetUserRequestToMoodle(username, String.valueOf(userMoodleId), delayed);
    }

    protected final void expectGetUserRequestToMoodle(final String username, final long userMoodleId) {
        expectGetUserRequestToMoodle(username, String.valueOf(userMoodleId), false);
    }

    protected final void expectGetUserRequestToMoodle(final String username, final String userMoodleId, boolean delayed) {
        final String response = String.format("[{\"id\":\"%s\", \"username\":\"%s\", \"email\":\"\", \"fullname\":\"\"}]", userMoodleId, username);
        expectGetUserRequestToMoodleWithResponse(username, response, delayed);
    }

    private void expectGetUserRequestToMoodleWithResponse(String username, String response, boolean delayed) {
        String payload = "wstoken=xxxx1234&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json&field=username&values%5B0%5D="
            + urlEncode(username);
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andExpect(content().string(payload))
            .andRespond(request -> {
                if (delayed) {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1001));
                    } catch (InterruptedException ignored) {
                        ;
                    }
                }
                return withSuccess(response, MediaType.APPLICATION_JSON).createResponse(request);
            });
    }

    protected final void expectCreateCourseRequestToMoodle(final String realisationId,
                                                           final String description, final long moodleCourseIdToReturn,
                                                           final String categoryId) {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andExpect(content().string(
                allOf(
                    startsWith(
                        "wstoken=xxxx1234&wsfunction=core_course_create_courses&moodlewsrestformat=json&courses%5B0%5D%5Bidnumber%5D="
                            + realisationId +
                            "&courses%5B0%5D%5Bfullname%5D=" +
                            "%3Cspan+lang%3D%22fi%22+class%3D%22multilang%22%3ELapsuus+ja+yhteiskunta%3C%2Fspan%3E" +
                            "%3Cspan+lang%3D%22sv%22+class%3D%22multilang%22%3EF%EF%BF%BDr+att+testa%3C%2Fspan%3E" +
                            "%3Cspan+lang%3D%22en%22+class%3D%22multilang%22%3EFor+testing%3C%2Fspan%3E" +
                            "&courses%5B0%5D%5Bshortname%5D=Lapsuus+ja"),
                    // The actual short name unique suffix is too difficult to check here, so we leave it out.
                    endsWith("&courses%5B0%5D%5Bcategoryid%5D=" + categoryId +
                        "&courses%5B0%5D%5Bsummary%5D=" + description + "&courses%5B0%5D%5Bvisible%5D=0" +
                        "&courses%5B0%5D%5Bstartdate%5D=1564952400&courses%5B0%5D%5Benddate%5D=1575496800" + // End date plus one month
                        "&courses%5B0%5D%5Bcourseformatoptions%5D%5B0%5D%5B" +
                        "name%5D=numsections&courses%5B0%5D%5Bcourseformatoptions%5D%5B0%5D%5Bvalue%5D=7"))))
            .andRespond(withSuccess("[{\"id\":\"" + moodleCourseIdToReturn + "\", \"shortname\":\"shortie\"}]", MediaType.APPLICATION_JSON));
    }

    protected void prepareMoodleGetCoursesResponseMock() {
        prepareMoodleGetCoursesResponseMock(MOODLE_COURSE_ID_IN_DB);
    }

    protected void prepareMoodleGetCoursesResponseMock(long moodleId) {
        prepareMoodleGetCoursesResponseBatchMock(Collections.singletonList(moodleId), false);
    }

    protected void prepareMoodleGetCoursesResponseMock(List<Long> moodleIds, boolean delayed) {
        int count = 1;
        int batchSize = 2;
        List<Long> moodleIdBatch = new ArrayList<>();
        for (Long moodleId: moodleIds) {
            moodleIdBatch.add(moodleId);
            if (count % batchSize == 0) {
                prepareMoodleGetCoursesResponseBatchMock(moodleIdBatch, delayed);
                moodleIdBatch.clear();
            }
            count++;
        }
        if (!moodleIdBatch.isEmpty()) {
            prepareMoodleGetCoursesResponseBatchMock(moodleIdBatch, delayed);
        }
    }

    protected void prepareMoodleGetCoursesResponseBatchMock(List<Long> moodleIds, boolean delayed) {
        String json = "[" + moodleIds.stream().map(moodleId -> "{ \"id\": \"" + moodleId + "\" }").collect(Collectors.joining(", ")) + "]";
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andExpect(content().string(startsWith("wstoken=xxxx1234&wsfunction=core_course_get_courses&moodlewsrestformat=json")))
            .andRespond(request -> {
                if (delayed) {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1001));
                    } catch (InterruptedException ignored) {
                        ;
                    }
                }
                return withSuccess(json, MediaType.APPLICATION_JSON).createResponse(request);
            });
    }

    private void mockGetEnrolledUsersForCoursesBatchWithError(List<MoodleCourseWithEnrollments> resultObjects, int failAtIndex) {
        // first the whole batch creates error
        moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
            .andExpect(content().string(startsWith(
                "wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users_with_capability&moodlewsrestformat=json")))
            .andRespond(request -> withServerError().createResponse(request));

        // then each item of the batch attempts request separately, and one of them creates error
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        for (int i = 0; i < resultObjects.size(); i++) {
            if (failAtIndex == i) {
                moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                    .andExpect(content().string(startsWith(
                        "wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users_with_capability&moodlewsrestformat=json")))
                    .andRespond(request -> withServerError().createResponse(request));
            } else {
                mockGetEnrolledUsersForCoursesBatch(Collections.singletonList(resultObjects.get(i)));
            }
        }
    }

    private void mockGetEnrolledUsersForCoursesBatch(List<MoodleCourseWithEnrollments> resultObjects) {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            final String json = ow.writeValueAsString(resultObjects);
            moodleReadOnlyMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andExpect(content().string(startsWith(
                    "wstoken=xxxx1234&wsfunction=core_enrol_get_enrolled_users_with_capability&moodlewsrestformat=json")))
                .andRespond(request -> withSuccess(json, MediaType.APPLICATION_JSON).createResponse(request));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    protected void prepareMoodleGetEnrolledUsersForCoursesMock(Long moodleId, List<MoodleUserEnrollments> enrollments) {
        prepareMoodleGetEnrolledUsersForCoursesMock(Collections.singletonList(new MoodleCourseWithEnrollments(moodleId, enrollments)), null);
    }

    protected void prepareMoodleGetEnrolledUsersForCoursesMock(List<MoodleCourseWithEnrollments> expected) {
        prepareMoodleGetEnrolledUsersForCoursesMock(expected, null);
    }

    protected void prepareMoodleGetEnrolledUsersForCoursesMock(List<MoodleCourseWithEnrollments> expected, Integer failAtIndex) {
        int count = 0;
        int batchSize = 2;
        boolean batchHasError = false;
        List<MoodleCourseWithEnrollments> resultObjectBatch = new ArrayList<>();
        for (MoodleCourseWithEnrollments course: expected) {
            resultObjectBatch.add(course);
            if (failAtIndex != null && failAtIndex == count) {
                batchHasError = true;
            }
            if (count % batchSize == 1) {
                if (batchHasError) {
                    mockGetEnrolledUsersForCoursesBatchWithError(resultObjectBatch, failAtIndex % batchSize);
                } else {
                    mockGetEnrolledUsersForCoursesBatch(resultObjectBatch);
                }
                resultObjectBatch.clear();
                batchHasError = false;
            }
            count++;
        }
        if (!resultObjectBatch.isEmpty()) {
            if (batchHasError) {
                mockGetEnrolledUsersForCoursesBatchWithError(resultObjectBatch, failAtIndex % batchSize);
            } else {
                mockGetEnrolledUsersForCoursesBatch(resultObjectBatch);
            }
        }
    }

    protected final MoodleUserEnrollments getMoodleUserEnrollments(int moodleUserId, String userName, int enrolledCourseId, long...roleIds) {
        MoodleUserEnrollments ret = new MoodleUserEnrollments();
        ret.id = (long) moodleUserId;
        ret.username = userName;
        ret.enrolledCourses = Collections.singletonList(new MoodleCourseData(enrolledCourseId));
        ret.roles = Arrays.stream(roleIds).mapToObj(MoodleRole::new).collect(Collectors.toList());
        return ret;
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

        enricherService.prefetchSisuCourses(SISU_COURSE_REALISATION_IDS);
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

    protected String generateMultiLangSpan(String langCode, String content) {
        return "<span lang=\"" + langCode + "\" class=\"multilang\">" + content + "</span>";
    }
}
