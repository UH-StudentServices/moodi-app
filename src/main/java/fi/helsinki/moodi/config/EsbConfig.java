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
import fi.helsinki.moodi.integration.esb.EsbClient;
import fi.helsinki.moodi.integration.http.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class EsbConfig {

    @Autowired
    private Environment environment;

    @Bean
    public EsbClient esbClient() {
        return new EsbClient(baseUrl(), esbRestTemplate());
    }

    @Bean
    public RestTemplate esbRestTemplate() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper());
        RestTemplate restTemplate = new RestTemplate(Collections.singletonList(converter));
        restTemplate.setInterceptors(Collections.singletonList(new LoggingInterceptor()));
        return restTemplate;
    }

    private String baseUrl() {
        return environment.getRequiredProperty("integration.esb.url");
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
