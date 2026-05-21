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
import fi.helsinki.moodi.integration.http.RequestTimingInterceptor;
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class MoodleConfig {
    private static final int RETRY_COUNT = 3;

    @Value("${httpClient.moodle.connectTimeout}")
    private int connectTimeout;

    @Value("${httpClient.moodle.socketTimeout}")
    private int socketTimeout;

    @Value("${integration.moodle.baseUrl}/webservice/rest/server.php")
    private String restUrl;

    @Value("${integration.moodle.wstoken}")
    private String wstoken;

    @Bean
    public MoodleClient moodleClient(
        ObjectMapper objectMapper,
        @Qualifier("moodleRestTemplate") RestTemplate moodleRestTemplate,
        @Qualifier("moodleReadOnlyRestTemplate") RestTemplate moodleReadOnlyRestTemplate) {
        return new MoodleClient(
            restUrl,
            wstoken,
            objectMapper,
            moodleRestTemplate,
            moodleReadOnlyRestTemplate
        );
    }

    private RestTemplate createRestTemplate(
        RestTemplateBuilder restTemplateBuilder,
        @Nullable HttpRequestRetryStrategy retryStrategy
    ) {
        PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();
        // Separate timeout for the socket before the connection is established. See https://jira.it.helsinki.fi/browse/MOODI-118.
        connectionManagerBuilder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(socketTimeout)).build());
        connectionManagerBuilder.setDefaultConnectionConfig(
            ConnectionConfig.custom().setConnectTimeout(Timeout.ofMilliseconds(socketTimeout)).build()
        );

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
            .setConnectionManager(connectionManagerBuilder.build())
            .setConnectionReuseStrategy((request, response, context) -> false);
        if (retryStrategy != null) {
            httpClientBuilder.setRetryStrategy(retryStrategy);
        }
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(
            httpClientBuilder.build()
        );

        factory.setConnectionRequestTimeout(Duration.ofMillis(connectTimeout));

        return restTemplateBuilder
            .interceptors(new RequestTimingInterceptor())
            .messageConverters(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new FormHttpMessageConverter()
            )
            .requestFactory(() -> new BufferingClientHttpRequestFactory(factory))
            .build();
    }

    //RestTemplate for requests that make modifications to Moodle. Do not get retried immediately in case of error.
    @Bean
    public RestTemplate moodleRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return createRestTemplate(restTemplateBuilder, null);
    }

    //RestTemplate for request that only read data from Moodle. May get retried up to RETRY_COUNT times in case of failure.
    @Bean
    public RestTemplate moodleReadOnlyRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return createRestTemplate(restTemplateBuilder, new HttpRequestRetryStrategy() {
            @Override
            public boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context) {
                return execCount <= RETRY_COUNT;
            }

            @Override
            public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
                return execCount <= RETRY_COUNT && response.getCode() >= 500;
            }

            @Override
            public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
                return TimeValue.ofSeconds(3);
            }
        });
    }

}
