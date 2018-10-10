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
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.helsinki.moodi.exception.IntegrationConnectionException;
import fi.helsinki.moodi.exception.MoodiException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static fi.helsinki.moodi.integration.moodle.MoodleClient.ResponseBodyEvaluator.Action.CONTINUE;
import static fi.helsinki.moodi.integration.moodle.MoodleClient.ResponseBodyEvaluator.Action.ERROR;
import static fi.helsinki.moodi.integration.moodle.MoodleClient.ResponseBodyEvaluator.Action.RETURN_NULL;
import static org.slf4j.LoggerFactory.getLogger;

public class MoodleClient {

    private static final Logger logger = getLogger(MoodleClient.class);

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final RestTemplate readOnlyRestTemplate;
    private final ObjectMapper objectMapper;
    private final String wstoken;
    protected String wsUsername; // protected non-final to facilitate testing

    public MoodleClient(String baseUrl,
                        String wstoken,
                        ObjectMapper objectMapper,
                        RestTemplate restTemplate,
                        RestTemplate readOnlyRestTemplate,
                        String wsUsername) {
        this.baseUrl = baseUrl;
        this.wstoken = wstoken;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.readOnlyRestTemplate = readOnlyRestTemplate;
        this.wsUsername = wsUsername;
    }

    public List<MoodleFullCourse> getCourses(List<Long> ids) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_course_get_courses");

        setListParameters(params, "options[ids][%s]", ids, String::valueOf);

        try {
            return execute(params, new TypeReference<List<MoodleFullCourse>>() {}, DEFAULT_EVALUATION, true);
        } catch (Exception e) {
            return handleException("Error executing method: core_course_get_courses", e);
        }
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

    // https://jira.it.helsinki.fi/browse/MOODI-84
    // We temporarily change the language of the WS user to that of the course to be created in order to
    // have the Announcements section name show up in the required language.
    // This method is synchronized, because can't have two threads doing that at the same time.
    public synchronized long createCourse(final MoodleCourse course) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_course_create_courses");

        params.set("courses[0][idnumber]", course.idnumber);
        params.set("courses[0][fullname]", course.fullName);
        params.set("courses[0][shortname]", course.shortName);
        params.set("courses[0][categoryid]", course.categoryId);
        params.set("courses[0][summary]", course.summary);
        params.set("courses[0][visible]", booleanToIntString(course.visible));
        params.set("courses[0][courseformatoptions][0][name]", "numsections");
        params.set("courses[0][courseformatoptions][0][value]", String.valueOf(course.numberOfSections));

        LanguageSwitch langSwitch = null;
        try {
            langSwitch = switchWsUserLang(course.langCode);

            return execute(params, new TypeReference<List<MoodleCourseData>>() {}, DEFAULT_EVALUATION, false)
                .stream()
                .findFirst()
                .map(s -> s.id)
                .orElse(null);
        } catch (Exception e) {
            return handleException("Error executing method: importCourse", e);
        } finally {
            revertWsUserLang(langSwitch);
        }
    }

    private LanguageSwitch switchWsUserLang(String newLang) {
        if (wsUsername != null && newLang != null) {
            MoodleUser wsUser = getUser(Arrays.asList(wsUsername));
            if (wsUser != null && wsUser.id != null && !newLang.equals(wsUser.lang)) {
                setUserLang(wsUser.id, newLang);
                return new LanguageSwitch(wsUser.id, wsUser.lang, newLang);
            }
        }
        return null;
    }

    private void revertWsUserLang(LanguageSwitch idAndLang) {
        if (idAndLang != null) {
            setUserLang(idAndLang.id, idAndLang.origLang);
        }
    }

    public void addEnrollments(final List<MoodleEnrollment> moodleEnrollments) {
        final MultiValueMap<String, String> params = createParametersForFunction("enrol_manual_enrol_users");

        for (int i = 0; i < moodleEnrollments.size(); i++) {
            final MoodleEnrollment moodleEnrollment = moodleEnrollments.get(i);
            params.set("enrolments[" + i + "][courseid]", String.valueOf(moodleEnrollment.moodleCourseId));
            params.set("enrolments[" + i + "][roleid]", String.valueOf(moodleEnrollment.moodleRoleId));
            params.set("enrolments[" + i + "][userid]", String.valueOf(moodleEnrollment.moodleUserId));
        }

        try {
            execute(params, new TypeReference<Void>() {}, EMPTY_OK_RESPONSE_EVALUATION, false);
        } catch (Exception e) {
            handleException("Error executing method: addEnrollments", e);
        }
    }

    public void removeEnrollments(final List<MoodleEnrollment> moodleEnrollments) {
        final MultiValueMap<String, String> params = createParametersForFunction("enrol_manual_unenrol_users");

        for (int i = 0; i < moodleEnrollments.size(); i++) {
            final MoodleEnrollment moodleEnrollment = moodleEnrollments.get(i);
            params.set("enrolments[" + i + "][courseid]", String.valueOf(moodleEnrollment.moodleCourseId));
            params.set("enrolments[" + i + "][userid]", String.valueOf(moodleEnrollment.moodleUserId));
        }

        try {
            execute(params, new TypeReference<Void>() {}, EMPTY_OK_RESPONSE_EVALUATION, false);
        } catch (Exception e) {
            handleException("Error executing method: removeEnrollments", e);
        }
    }

    @Cacheable(value = "moodle-client.moodle-user-by-username", unless = "#result == null")
    public MoodleUser getUser(final List<String> username) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_user_get_users_by_field");
        params.set("field", "username");
        setListParameters(params, "values[%s]", username, String::valueOf);

        try {
            return execute(params, new TypeReference<List<MoodleUser>>() {}, DEFAULT_EVALUATION, true)
                .stream()
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return handleException("Error executing method: getUsers", e);
        }
    }

    // For testing purposes
    public long createUser(final String username, final String firstName, final String lastName,
                           final String email, final String password, final String idNumber) {

        final MultiValueMap<String, String> params = createParametersForFunction("core_user_create_users");

        params.set("users[0][username]", username);
        params.set("users[0][password]", password);
        params.set("users[0][firstname]", firstName);
        params.set("users[0][lastname]", lastName);
        params.set("users[0][email]", email);
        params.set("users[0][idnumber]", idNumber);

        try {
            return Long.valueOf(execute(params, new TypeReference<List<Map<String, String>>>() {},
                    DEFAULT_EVALUATION, false).get(0).get("id"));
        } catch (IOException e) {
            return handleException("Error executing method: createUser", e);
        }
    }

    public void deleteUser(final long moodleId) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_user_delete_users");

        params.set("userids[0]", String.valueOf(moodleId));

        try {
            execute(params, new TypeReference<Void>() {}, DEFAULT_EVALUATION, false);
        } catch (IOException e) {
            handleException("Error executing method: deleteUser", e);
        }
    }

    public List<MoodleUserEnrollments> getEnrolledUsers(final long courseId) {
        final MultiValueMap<String, String> params = createParametersForFunction("core_enrol_get_enrolled_users");
        params.set("courseid", String.valueOf(courseId));

        try {
            return execute(params, new TypeReference<List<MoodleUserEnrollments>>() {}, DEFAULT_EVALUATION, true);
        } catch (Exception e) {
            return handleException("Error executing method: getEnrolledUsers", e);
        }
    }

    public void setUserLang(final long userId, final String langCode) {
        logger.info("Setting language of user '{}' to '{}'", userId, langCode);

        final MultiValueMap<String, String> params = createParametersForFunction("core_user_update_users");

        params.set("users[0][id]", String.valueOf(userId));
        params.set("users[0][lang]", langCode);

        try {
            execute(params, new TypeReference<List<Map<String, String>>>() {},
                    DEFAULT_EVALUATION, false);
        } catch (Throwable e) {
            handleException("Error executing method: setUserLang", new Exception(e));
        }
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
            params.set(array + "[" + i + "][userid]", String.valueOf(moodleEnrollment.moodleUserId));
            params.set(array + "[" + i + "][roleid]", String.valueOf(moodleEnrollment.moodleRoleId));
            params.set(array + "[" + i + "][instanceid]", String.valueOf(moodleEnrollment.moodleCourseId));
            params.set(array + "[" + i + "][contextlevel]", "course");
        }

        try {
            execute(params, null, EMPTY_OK_RESPONSE_EVALUATION, false);
        } catch (Exception e) {
            handleException("Error executing method: assignRoles", e);
        }
    }

    private <T> T handleException(final String message, final Exception e) {
        if (e instanceof MoodiException) {
            throw (MoodiException) e;
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
            final ResponseBodyEvaluator responseBodyEvaluator,
            final boolean readOnly)
            throws IOException {

        logger.info("Invoke url: {} with params: {}", baseUrl, paramsToString(params));

        final String body = getRestTemplate(readOnly)
            .postForObject(baseUrl, new HttpEntity<>(params, createHeaders()), String.class);

        logger.debug("Got response body:\n{}", body);

        switch (responseBodyEvaluator.evaluate(body)) {
            case CONTINUE:
                break;
            case ERROR:
                throw createMoodleClientException(body);
            case RETURN_NULL:
                return null;
            case RETURN_BODY:
                return (T) body;
            default:
        }

        try {
            return objectMapper.readValue(body, typeReference);
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("Moodle connection failure", e);
        } catch (Exception e) {
            throw createMoodleClientException(body);
        }
    }

    private MoodleClientException createMoodleClientException(final String body) throws IOException {
        logger.error("Got unexpected response body " + body);
        final Map<String, String> map = objectMapper.readValue(body, Map.class);
        return new MoodleClientException(map.get("message"), map.get("exception"), map.get("errorcode"));
    }

    @FunctionalInterface
    protected interface ResponseBodyEvaluator {

        enum Action {
            CONTINUE,
            RETURN_BODY,
            RETURN_NULL,
            ERROR
        }

        Action evaluate(String responseBody);
    }

    private static ResponseBodyEvaluator DEFAULT_EVALUATION = s -> CONTINUE;

    private static ResponseBodyEvaluator EMPTY_OK_RESPONSE_EVALUATION =  s -> StringUtils.isEmpty(s) || "null".equals(s) ? RETURN_NULL : ERROR;

    private static String paramsToString(final MultiValueMap<String, String> params) {
        final StringBuilder sb = new StringBuilder();
        for (final String name : params.keySet()) {
            sb.append(name).append(": ").append(params.get(name));
        }

        return sb.toString();
    }

    private class LanguageSwitch {
        public Long id;
        public String origLang;
        public String newLang;

        public LanguageSwitch(Long id, String origLang, String newLang) {
            this.id = id;
            this.origLang = origLang;
            this.newLang = newLang;
        }
    }
}
