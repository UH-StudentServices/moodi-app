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

package fi.helsinki.moodi.integration.iam;

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

public class IAMRestClient implements IAMClient {
    private static final Logger logger = getLogger(IAMRestClient.class);

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public IAMRestClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "iam-client.student-username-by-student-number", unless = "#result == null or #result.size()==0")
    public List<String> getStudentUserNameList(final String studentNumber) {
        logger.debug("Get student username by student number {}", studentNumber);

        try {
            List<IAMStudent> result = restTemplate.exchange(
                String.format("%s/iam/findStudent/%s", baseUrl, studentNumber),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IAMStudent>>() {})
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
            throw new IntegrationConnectionException("IAM connection failure", e);
        }
    }

    @Cacheable(value = "iam-client.teacher-username-by-teacher-id", unless = "#result == null or #result.size()==0")
    public List<String> getTeacherUserNameList(final String employeeNumber) {
        logger.debug("Get teacher username by teacher id {}", employeeNumber);

        try {
            List<IAMEmployee> result = restTemplate.exchange(
                String.format("%s/iam/findEmployee/%s", baseUrl, employeeNumber),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IAMEmployee>>() {})
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
            throw new IntegrationConnectionException("IAM connection failure", e);
        }
    }
}
