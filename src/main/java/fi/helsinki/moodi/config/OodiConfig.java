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
import fi.helsinki.moodi.integration.oodi.OodiClient;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.KeyStore;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;

@Configuration
public class OodiConfig {

    @Autowired
    private Environment environment;

    @Bean
    public OodiClient oodiClient() {
        return new OodiClient(baseUrl(), oodiRestTemplate());
    }

    private KeyStore oodiKeyStore(String keystoreLocation, char[] keystorePassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        FileSystemResource keystoreFile = new FileSystemResource(
                new File(keystoreLocation));

        keyStore.load(keystoreFile.getInputStream(), keystorePassword);
        return keyStore;
    }

    private boolean useClientCert() {
        return environment.containsProperty("httpClient.keystoreLocation") && environment.containsProperty("httpClient.keystorePassword");
    }

    private SSLContext sslContext() {
        String keystoreLocation = environment.getRequiredProperty("httpClient.keystoreLocation");
        String keystorePassword = environment.getRequiredProperty("httpClient.keystorePassword");
        char[] keystorePasswordCharArray = keystorePassword.toCharArray();

        try {
            return SSLContextBuilder.create()
                    .loadKeyMaterial(oodiKeyStore(keystoreLocation, keystorePasswordCharArray), keystorePasswordCharArray).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load client keystore");
        }
    }

    @Bean
    public RestTemplate oodiRestTemplate() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper());

        RestTemplate restTemplate = new RestTemplate(Collections.singletonList(converter));
        restTemplate.setInterceptors(newArrayList(new LoggingInterceptor(), new RequestTimingInterceptor()));
        if (useClientCert()) {
            final HttpClient client = HttpClients.custom().setSSLContext(sslContext()).build();
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
        }
        return restTemplate;
    }

    private String baseUrl() {
        return environment.getProperty("integration.oodi.url");
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
