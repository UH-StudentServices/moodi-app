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
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
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
    public List<String> getStudentUsernameList(final String studentNumber) {
        LOGGER.debug("Get student username by student number {}", studentNumber);

        try {
            List<EsbStudent> result = restTemplate.exchange(
                String.format("%s/iam/findStudent/%s", baseUrl, studentNumber),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<EsbStudent>>() {})
                .getBody();

            if (result != null){
                return result
                    .stream()
                    .map(s -> s.username)
                    .collect(Collectors.toList());
            } else {
                return newArrayList();
            }
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("ESB connection failure", e);
        }
    }

    @Cacheable(value="esb-client.teacher-username-by-teacher-id", unless="#result == null")
    public List<String> getTeacherUsernameList(final String teacherId) {
        LOGGER.debug("Get teacher username by teacher id {}", teacherId);

        try {
            List<EsbEmployee> result = restTemplate.exchange(
                String.format("%s/iam/findEmployee/%s", baseUrl, teacherId),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<EsbEmployee>>() {})
                .getBody();

            if (result != null) {
                return result
                    .stream()
                    .map(s -> s.username)
                    .collect(Collectors.toList());
            } else {
                return newArrayList();
            }
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("ESB connection failure", e);
        }
    }
}
