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
import fi.helsinki.moodi.integration.http.LoggingInterceptor;
import fi.helsinki.moodi.integration.http.RequestTimingInterceptor;
import fi.helsinki.moodi.integration.oodi.OodiClient;
import fi.helsinki.moodi.integration.sisu.SisuClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.KeyStore;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;

@Configuration
public class StudyRegistryConfig {
    @Value("${httpClient.connectTimeout}")
    private int connectTimeout;

    @Value("${httpClient.socketTimeout}")
    private int socketTimeout;

    @Autowired
    private Environment environment;

    @Bean
    public OodiClient oodiClient(RestTemplate studyRegistryRestTemplate) {
        return new OodiClient(environment.getProperty("integration.oodi.url"), studyRegistryRestTemplate);
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

    @PostConstruct
    public void initDefaultSSLContext() {
        if (useClientCert()) {
            SSLContext sslContext = sslContext();

            if (sslContext != null) {
                SSLContext.setDefault(sslContext);
            }
        }
    }

    private SSLContext sslContext() {
        String keystoreLocation = environment.getRequiredProperty("httpClient.keystoreLocation");
        String keystorePassword = environment.getRequiredProperty("httpClient.keystorePassword");
        char[] keystorePasswordCharArray = keystorePassword.toCharArray();

        try {
            return SSLContextBuilder.create()
                    .loadKeyMaterial(oodiKeyStore(keystoreLocation, keystorePasswordCharArray), keystorePasswordCharArray).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load client keystore " + keystoreLocation, e);
        }
    }

    @Bean
    public RestTemplate studyRegistryRestTemplate(ObjectMapper objectMapper, HttpClientBuilder clientBuilder) {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        RestTemplate restTemplate = new RestTemplate(Collections.singletonList(converter));
        restTemplate.setInterceptors(newArrayList(new LoggingInterceptor(), new RequestTimingInterceptor()));

        if (useClientCert()) {
            clientBuilder.setSSLContext(sslContext());
        }
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(clientBuilder.build()));
        return restTemplate;
    }

    @Bean
    public SisuClient sisuClient(RestTemplate studyRegistryRestTemplate) {
        return new SisuClient(environment.getProperty("integration.sisu.baseUrl"),
                environment.getProperty("integration.sisu.apiKey"),
                connectTimeout,
                socketTimeout,
                studyRegistryRestTemplate);
    }
}
