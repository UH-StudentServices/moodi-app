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

import fi.helsinki.moodi.integration.sisu.SisuClient;
import jakarta.annotation.PostConstruct;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.KeyStore;

@Configuration
public class StudyRegistryConfig {
    @Value("${httpClient.sisu.connectTimeout}")
    private int connectTimeout;

    @Value("${httpClient.sisu.socketTimeout}")
    private int socketTimeout;

    @Value("${httpClient.sisu.keystoreLocation:#{null}}")
    private String keystoreLocation;

    @Value("${httpClient.sisu.keystorePassword:#{null}}")
    private String keystorePassword;

    @Value("${integration.sisu.baseUrl}")
    private String sisuBaseUrl;

    @Value("${integration.sisu.apiKey}")
    private String sisuApiKey;

    private KeyStore keyStore(String keystoreLocation, char[] keystorePassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        FileSystemResource keystoreFile = new FileSystemResource(
                new File(keystoreLocation));

        keyStore.load(keystoreFile.getInputStream(), keystorePassword);
        return keyStore;
    }

    private boolean useClientCert() {
        return keystoreLocation != null && keystorePassword != null;
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
        char[] keystorePasswordCharArray = keystorePassword.toCharArray();

        try {
            return SSLContextBuilder.create()
                    .loadKeyMaterial(keyStore(keystoreLocation, keystorePasswordCharArray), keystorePasswordCharArray).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load client keystore " + keystoreLocation, e);
        }
    }

    @Bean
    public RestTemplate studyRegistryRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    public SisuClient sisuClient(@Qualifier("studyRegistryRestTemplate") RestTemplate studyRegistryRestTemplate) {
        return new SisuClient(
            sisuBaseUrl,
            sisuApiKey,
            connectTimeout,
            socketTimeout,
            studyRegistryRestTemplate
        );
    }
}
