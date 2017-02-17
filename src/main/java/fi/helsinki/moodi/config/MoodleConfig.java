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
import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.http.RequestTimingInterceptor;
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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

import static java.util.Collections.singletonList;

@Configuration
public class MoodleConfig {

    private static final int RETRY_COUNT = 3;

    @Autowired
    private Environment environment;

    @Bean
    public MoodleClient moodleClient() {
        final ObjectMapper objectMapper = objectMapper();
        return new MoodleClient(baseUrl(), wstoken(), objectMapper, moodleRestTemplate());
    }

    @Bean
    public RestTemplate moodleRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        restTemplate.setInterceptors(singletonList(new RequestTimingInterceptor()));

        restTemplate.setMessageConverters(
                Lists.newArrayList(
                        new StringHttpMessageConverter(),
                        new FormHttpMessageConverter()));

        return restTemplate;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(Integer.valueOf(environment.getProperty("httpClient.readTimeout")));
        factory.setConnectTimeout(Integer.valueOf(environment.getProperty("httpClient.connectTimeout")));

        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(
            Integer.valueOf(environment.getProperty("httpClient.maxTotal")));
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(
            Integer.valueOf(environment.getProperty("httpClient.defaultMaxPerRoute")));

        CloseableHttpClient httpClient = HttpClientBuilder.create()
            .setConnectionManager(poolingHttpClientConnectionManager)
            .build();

        factory.setHttpClient(httpClient);

        return new BufferingClientHttpRequestFactory(factory);
    }


    private String baseUrl() {
        return environment.getRequiredProperty("integration.moodle.url");
    }

    private String wstoken() {
        return environment.getRequiredProperty("integration.moodle.wstoken");
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
