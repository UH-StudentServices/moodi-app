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

package fi.helsinki.moodi.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.helsinki.moodi.integration.http.LoggingInterceptor;
import fi.helsinki.moodi.integration.http.RequestTimingInterceptor;
import fi.helsinki.moodi.integration.iam.IAMClient;
import fi.helsinki.moodi.integration.iam.IAMMockClient;
import fi.helsinki.moodi.integration.iam.IAMRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Configuration
@PropertySource(value = {"file:/opt/moodi/config/iammockusers.properties"}, ignoreResourceNotFound = true
)
public class IAMConfig {

    @Value("${integration.iam.url:#{null}}")
    private String baseUrl;

    @Value("${integration.iam.mock:false}")
    private boolean mockClientImplementation;

    @Value("${integration.iam.users:#{null}}")
    private String mockUsers;

    @Bean
    public IAMClient iamClient() {
        if (mockClientImplementation) {
            return new IAMMockClient(getMockUsers());
        } else {
            return new IAMRestClient(baseUrl, iamRestTemplate());
        }
    }

    private Map<String, String> getMockUsers() {
        if (mockUsers != null) {
            return newArrayList(mockUsers.split(","))
                .stream()
                .map(u -> u.split(":"))
                .collect(Collectors.toMap(u -> u[0], u -> u[1]));
        }

        return new HashMap<>();
    }

    @Bean
    public RestTemplate iamRestTemplate() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper());
        RestTemplate restTemplate = new RestTemplate(Collections.singletonList(converter));
        restTemplate.setInterceptors(newArrayList(new LoggingInterceptor(), new RequestTimingInterceptor()));
        return restTemplate;
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
