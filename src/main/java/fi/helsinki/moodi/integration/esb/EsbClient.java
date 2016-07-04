package fi.helsinki.moodi.integration.esb;

import fi.helsinki.moodi.exception.IntegrationConnectionException;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public class EsbClient {

    private static final Logger LOGGER = getLogger(EsbClient.class);

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public EsbClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    @Cacheable(value="esb-client.student-username-by-student-number", unless="#result == null")
    public String getStudentUsername(final String studentNumber) {
        LOGGER.debug("Get student username by student number {}", studentNumber);

        try {
            Optional<List<EsbStudent>> result = Optional.ofNullable(restTemplate.exchange(
                    "{baseUrl}/iam/findStudent/{studentNumber}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EsbStudent>>() {
                    },
                    baseUrl,
                    studentNumber)
                .getBody());

            return
                result
                    .filter(r -> r.size() > 0)
                    .map(r -> r.get(0).username)
                    .orElse(null);
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("ESB connection failure", e);
        }
    }
    @Cacheable(value="esb-client.teacher-username-by-teacher-id", unless="#result == null")
    public String getTeacherUsername(final String teacherId) {
        LOGGER.debug("Get teacher username by teacher id {}", teacherId);

        try {
            Optional<List<EsbEmployee>> result = Optional.ofNullable(restTemplate.exchange(
                    "{baseUrl}/iam/findEmployee/{employeeId}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EsbEmployee>>() {},
                    baseUrl,
                    teacherId)
                .getBody());

                return
                    result
                        .filter(r -> r.size() > 0)
                        .map(r -> r.get(0).username)
                        .orElse(null);
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("ESB connection failure", e);
        }
    }
}
