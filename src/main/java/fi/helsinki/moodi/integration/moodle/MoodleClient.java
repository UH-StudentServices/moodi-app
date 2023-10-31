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

package fi.helsinki.moodi.integration.moodle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.slf4j.LoggerFactory.getLogger;

public class MoodleClient {

    private static final Logger logger = getLogger(MoodleClient.class);

    private final String restUrl;
    private final RestTemplate restTemplate;
    private final RestTemplate readOnlyRestTemplate;
    private final ObjectMapper objectMapper;
    private final String wstoken;
    @Value("${MoodleClient.batchsize:100}")
    private int batchSize;

    private static final String ENROLMENTS = "enrolments";
    private static final String COURSEID = "courseid";
    private static final String ROLEID = "roleid";
    private static final String USERID = "userid";
    private static final String SUSPEND = "suspend";
    private static final String COURSES = "courses";
    private static final String USERS = "users";

    public MoodleClient(String restUrl,
                        String wstoken,
                        ObjectMapper objectMapper,
                        RestTemplate restTemplate,
                        RestTemplate readOnlyRestTemplate) {
        this.restUrl = restUrl;
        this.wstoken = wstoken;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.readOnlyRestTemplate = readOnlyRestTemplate;
    }

    public List<MoodleFullCourse> getCourses(List<Long> ids) {
        List<MoodleFullCourse> ret = new ArrayList<>();
        List<MoodleFullCourse> result;
        int batchCounter = 1;
        List<List<Long>> batches = splitToBatches(ids);
        for (List<Long> batchIds : batches) {
            final MultiValueMap<String, String> params = createParametersForFunction("core_course_get_courses");

            setListParameters(params, "options[ids][%s]", batchIds, String::valueOf);

            try {
                result = execute(params, new TypeReference<List<MoodleFullCourse>>() {}, true);
            } catch (Exception e) {
                return handleException("Error executing method: getCourses (batch " + batchCounter + "/" + batches.size() + ")", e);
            }
            if (result != null) {
                ret.addAll(result);
            }
            batchCounter++;
        }
        return ret;
    }

    private <T> void setListParameters(
            final MultiValueMap<String, String> params,
            final String paramTemplate,
            final Collection<T> values,
            final Function<T, String> toStringConverter) {

        final List<T> list = new ArrayList<>(values);
        for (int i = 0; i < list.size(); i++) {
            final String name = String.format(paramTemplate, i);
            final T value = list.get(i);
            params.set(name, toStringConverter.apply(value));
        }
    }

    public long createCourse(final MoodleCourse course) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_course_create_courses");

        params.set(createParamName(COURSES, "idnumber", 0), course.idNumber);
        params.set(createParamName(COURSES, "fullname", 0), course.fullName);
        params.set(createParamName(COURSES, "shortname", 0), course.shortName);
        params.set(createParamName(COURSES, "categoryid", 0), course.categoryId);
        params.set(createParamName(COURSES, "summary", 0), course.summary);
        params.set(createParamName(COURSES, "visible", 0), booleanToIntString(course.visible));
        params.set(createParamName(COURSES, "startdate", 0), localDateToString(course.startTime));
        params.set(createParamName(COURSES, "enddate", 0), localDateToString(course.endTime));
        params.set(createParamName(COURSES, "courseformatoptions", 0) + "[0][name]", "numsections");
        params.set(createParamName(COURSES, "courseformatoptions", 0) + "[0][value]", String.valueOf(course.numberOfSections));

        try {
            return execute(params, new TypeReference<List<MoodleCourseData>>() {}, false)
                .stream()
                .findFirst()
                .map(s -> s.id)
                .orElse(null);
        } catch (Exception e) {
            return handleException("Error executing method: createCourse", e);
        }
    }

    private String localDateToString(LocalDate d) {
        return "" + d.atStartOfDay(ZoneId.of("Europe/Helsinki")).toEpochSecond();
    }

    private List<List<Long>> splitToBatches(final List<Long> ids) {
        final List<List<Long>> batches = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger();
        List<Long> batch = new ArrayList<>();
        for (Long id : ids) {
            if (counter.getAndIncrement() % batchSize == 0) {
                batch = new ArrayList<>();
                batches.add(batch);
            }
            batch.add(id);
        }
        return batches;
    }

    public void addEnrollments(final List<MoodleEnrollment> moodleEnrollments) {
        final MultiValueMap<String, String> params = createParametersForFunction("enrol_manual_enrol_users");

        for (int i = 0; i < moodleEnrollments.size(); i++) {
            final MoodleEnrollment moodleEnrollment = moodleEnrollments.get(i);
            params.set(createParamName(ENROLMENTS, COURSEID, i), String.valueOf(moodleEnrollment.moodleCourseId));
            params.set(createParamName(ENROLMENTS, ROLEID, i), String.valueOf(moodleEnrollment.moodleRoleId));
            params.set(createParamName(ENROLMENTS, USERID, i), String.valueOf(moodleEnrollment.moodleUserId));
        }

        try {
            execute(params, null, false);
        } catch (Exception e) {
            handleException("Error executing method: addEnrollments", e);
        }
    }

    public void suspendEnrollments(final List<MoodleEnrollment> moodleEnrollments) {
        final MultiValueMap<String, String> params = createParametersForFunction("enrol_manual_enrol_users");

        for (int i = 0; i < moodleEnrollments.size(); i++) {
            final MoodleEnrollment moodleEnrollment = moodleEnrollments.get(i);
            params.set(createParamName(ENROLMENTS, COURSEID, i), String.valueOf(moodleEnrollment.moodleCourseId));
            params.set(createParamName(ENROLMENTS, ROLEID, i), String.valueOf(moodleEnrollment.moodleRoleId));
            params.set(createParamName(ENROLMENTS, USERID, i), String.valueOf(moodleEnrollment.moodleUserId));
            params.set(createParamName(ENROLMENTS, SUSPEND, i), "1");
        }

        try {
            execute(params, null, false);
        } catch (Exception e) {
            handleException("Error executing method: suspendEnrollments", e);
        }
    }

    @Cacheable(value = "moodle-client.moodle-user-by-username", unless = "#result == null")
    public MoodleUser getUser(final List<String> username) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_user_get_users_by_field");
        params.set("field", "username");
        setListParameters(params, "values[%s]", username, String::valueOf);

        try {
            return execute(params, new TypeReference<List<MoodleUser>>() {}, true)
                .stream()
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return handleException("Error executing method: getUser", e);
        }
    }

    // For testing purposes
    public long createUser(final String username, final String firstName, final String lastName,
                           final String email, final String password, final String idNumber) {

        final MultiValueMap<String, String> params = createParametersForFunction("core_user_create_users");

        params.set(createParamName(USERS, "username", 0), username);
        params.set(createParamName(USERS, "password", 0), password);
        params.set(createParamName(USERS, "firstname", 0), firstName);
        params.set(createParamName(USERS, "lastname", 0), lastName);
        params.set(createParamName(USERS, "email", 0), email);
        params.set(createParamName(USERS, "idnumber", 0), idNumber);

        try {
            return Long.parseLong(execute(params, new TypeReference<List<Map<String, String>>>() {},
                false).get(0).get("id"));
        } catch (Exception e) {
            return handleException("Error executing method: createUser", e);
        }
    }

    public void deleteUser(final long moodleId) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_user_delete_users");

        params.set("userids[0]", String.valueOf(moodleId));

        try {
            execute(params, null, false);
        } catch (Exception e) {
            handleException("Error executing method: deleteUser", e);
        }
    }

    public List<MoodleUserEnrollments> getEnrolledUsers(final long courseId) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_enrol_get_enrolled_users");
        params.set(COURSEID, String.valueOf(courseId));

        try {
            return execute(params, new TypeReference<List<MoodleUserEnrollments>>() {}, true);
        } catch (Exception e) {
            return handleException("Error executing method: getEnrolledUsers", e);
        }
    }

    public void getEnrolledUsersForCourses(final Map<Long, List<MoodleUserEnrollments>> enrolmentsByCourseId, final List<Long> courseIds) {
        int batchCounter = 1;
        List<List<Long>> batches = splitToBatches(courseIds);
        for (List<Long> batchCourseIds : batches) {
            List<MoodleCourseWithEnrollments> result = null;
            MultiValueMap<String, String> params = createEnrolmentQueryParams(batchCourseIds);
            try {
                result = execute(params, new TypeReference<List<MoodleCourseWithEnrollments>>() {
                }, true);
                if (result != null && result.size() != batchCourseIds.size()) {
                    throw new MoodleClientException("Received response with less courses (" + result.size() +
                        ") than sent batchCourseIds: " + batchCourseIds.size(), "", "500");
                }
            } catch (Exception e) {
                result = null;
                logger.info("Error executing method: getEnrolledUsersForCourses (batch " + batchCounter + "/" + batches.size() + ")", e);
                logger.info("attempt calling course enrollments one course at time");
                int errorsInBatch = 0;
                for (Long courseId : batchCourseIds) {
                    params = createEnrolmentQueryParams(Collections.singletonList(courseId));
                    List<MoodleCourseWithEnrollments> singleResult = null;
                    try {
                        singleResult = execute(params, new TypeReference<List<MoodleCourseWithEnrollments>>() {
                        }, true);
                        if (singleResult != null && singleResult.size() != 1) {
                            throw new MoodleClientException("Received response with no courses for courseId " + courseId, "", "500");
                        }
                    } catch (Exception ee) {
                        errorsInBatch++;
                        logger.info("received error when attempting to get enrolments for course id " + courseId, ee);
                    }
                    if (singleResult != null && singleResult.size() == 1) {
                        List<MoodleUserEnrollments> enrollments = singleResult.get(0).users;
                        enrolmentsByCourseId.put(courseId, enrollments);
                    }
                }
                if (errorsInBatch > 50) {
                    handleException("Too many errors in one batch, abort: " + errorsInBatch + " (batch " + batchCounter +
                        "/" + batches.size() + ")", e);
                }
            }
            if (result != null) { // result.size() == batchCourseIds.size() always at this point
                for (int i = 0; i < batchCourseIds.size(); i++) {
                    long courseId = batchCourseIds.get(i);
                    List<MoodleUserEnrollments> enrollments = result.get(i).users;
                    enrolmentsByCourseId.put(courseId, enrollments);
                }
            }
            batchCounter++;
        }
    }

    public long updateCourseVisibility(final long courseId, final boolean visible) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_course_update_courses");
        // courses[0][id]= courseId
        // courses[0][visible]= int
        params.set(createParamName(COURSES, "id", 0), String.valueOf(courseId));
        params.set(createParamName(COURSES, "visible", 0), booleanToIntString(visible));
        try {
            execute(params, null, false);
            return courseId;
        } catch (Exception e) {
            return handleException("Error executing method: updateCourseVisibility", e);
        }
    }

    private MultiValueMap<String, String> createEnrolmentQueryParams(List<Long> batchCourseIds) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_enrol_get_enrolled_users_with_capability");
        setListParameters(params, "coursecapabilities[%s][courseid]", batchCourseIds, String::valueOf);
        setListParameters(params, "coursecapabilities[%s][capabilities][0]", batchCourseIds, x -> "");
        params.set("options[0][name]", "userfields");
        params.set("options[0][value]", "id, username, roles, enrolledcourses");
        return params;
    }

    public void addRoles(final List<MoodleEnrollment> moodleEnrollments) {
        assignRoles(moodleEnrollments, true);
    }

    public void removeRoles(final List<MoodleEnrollment> moodleEnrollments) {
        assignRoles(moodleEnrollments, false);
    }

    private void assignRoles(final List<MoodleEnrollment> moodleEnrollments, final boolean addition) {
        final String function = addition ? "core_role_assign_roles" : "core_role_unassign_roles";
        final String array = addition ? "assignments" : "unassignments";

        final MultiValueMap<String, String> params = createParametersForFunction(function);

        for (int i = 0; i < moodleEnrollments.size(); i++) {
            final MoodleEnrollment moodleEnrollment = moodleEnrollments.get(i);
            params.set(createParamName(array, USERID, i), String.valueOf(moodleEnrollment.moodleUserId));
            params.set(createParamName(array, ROLEID, i), String.valueOf(moodleEnrollment.moodleRoleId));
            params.set(createParamName(array, "instanceid", i), String.valueOf(moodleEnrollment.moodleCourseId));
            params.set(createParamName(array, "contextlevel", i), "course");
        }

        try {
            execute(params, null, false);
        } catch (Exception e) {
            handleException("Error executing method: assignRoles", e);
        }
    }

    private <T> T handleException(final String message, final Exception e) {
        if (e instanceof MoodleClientException) {
            throw (MoodleClientException) e;
        } else {
            throw new MoodleClientException(message, e);
        }
    }

    private String booleanToIntString(final boolean b) {
        return b ? "1" : "0";
    }

    private MultiValueMap<String, String> createParametersForFunction(final String function) {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("wstoken", wstoken);
        params.set("wsfunction", function);
        params.set("moodlewsrestformat", "json");
        return params;
    }

    private String createParamName(String param1, String param2, int i) {
        // "enrolments", "courseid", 0 -> "enrolments[0][courseid]"
        return String.format("%s[%d][%s]", param1, i, param2);
    }

    private HttpHeaders createHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private RestTemplate getRestTemplate(final boolean readOnly) {
        return readOnly ? readOnlyRestTemplate : restTemplate;
    }

    private <T> T execute(
            final MultiValueMap<String, String> params,
            final TypeReference<T> typeReference,
            final boolean readOnly)
            throws IOException {

        logger.info("Invoke url: {} with params: {}", restUrl, paramsToString(params));

        final String body = getRestTemplate(readOnly)
            .postForObject(restUrl, new HttpEntity<>(params, createHeaders()), String.class);

        if (body != null && !body.isEmpty()) {
            logger.info("Response body: {}", body.length() > 300 ? body.substring(0, 300) + "..." : body);
            final JsonNode jsonNode = objectMapper.readTree(body);

            // Checking if there is a Moodle exception in the response body. Moodle returns 200 OK even if there is an
            // exception.
            if (jsonNode.has("exception")) {
                String exception = jsonNode.get("exception").asText();
                String message = Optional.of(jsonNode.get("message")).map(JsonNode::asText).orElse("");
                String errorCode = Optional.of(jsonNode.get("errorcode")).map(JsonNode::asText).orElse("");
                throw new MoodleClientException(message, exception, errorCode);
            }
        } else {
            logger.info("Response body is empty");
        }

        // No mapping requested, return null
        if (typeReference == null) {
            return null;
        }

        return objectMapper.readValue(body, typeReference);
    }

    private static String paramsToString(final MultiValueMap<String, String> params) {
        final StringBuilder sb = new StringBuilder();
        for (final String name : params.keySet()) {
            List<String> values = name.equals("wstoken") ? Collections.singletonList("xxxx") : params.get(name);
            sb.append(name).append(": ").append(values).append("\n");
        }

        return sb.toString();
    }
}
