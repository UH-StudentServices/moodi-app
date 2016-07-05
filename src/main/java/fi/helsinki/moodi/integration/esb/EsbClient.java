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
