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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.http.RequestTimingInterceptor;
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Configuration
public class MoodleConfig {
    private static final int RETRY_COUNT = 3;

    @Autowired
    private Environment environment;

    @Bean
    public MoodleClient moodleClient(ObjectMapper objectMapper, HttpClientBuilder clientBuilder) {
        return new MoodleClient(
            restUrl(),
            wstoken(),
            objectMapper,
            moodleRestTemplate(clientBuilder),
            moodleReadOnlyRestTemplate(clientBuilder));
    }

    private RestTemplate createRestTemplate(HttpRequestRetryHandler httpRequestRetryHandler, HttpClientBuilder clientBuilder) {
        final HttpClient httpClient = clientBuilder
            .setRetryHandler(httpRequestRetryHandler)
            .build();

        final ClientHttpRequestFactory requestFactory =
            new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestTimingInterceptor()));

        restTemplate.setMessageConverters(
            Lists.newArrayList(
                new StringHttpMessageConverter(),
                new FormHttpMessageConverter()));

        return restTemplate;
    }

    //RestTemplate for requests that make modifications to Moodle. Do not get retried immediately in case of error.
    @Bean
    public RestTemplate moodleRestTemplate(HttpClientBuilder clientBuilder) {
        RestTemplate template = createRestTemplate(new DefaultHttpRequestRetryHandler(RETRY_COUNT, false), clientBuilder);
        template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return template;
    }

    //RestTemplate for request that only read data from Moodle. May get retried up to RETRY_COUNT times in case of failure.
    @Bean
    public RestTemplate moodleReadOnlyRestTemplate(HttpClientBuilder clientBuilder) {
        RestTemplate template = createRestTemplate(new DefaultHttpRequestRetryHandler(RETRY_COUNT, true), clientBuilder);
        template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return template;
    }

    private String restUrl() {
        return environment.getProperty("integration.moodle.baseUrl") + "/webservice/rest/server.php";
    }

    private String wstoken() {
        return environment.getRequiredProperty("integration.moodle.wstoken");
    }
}
