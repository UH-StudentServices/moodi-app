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

import fi.helsinki.moodi.interceptor.AccessLoggingInterceptor;
import fi.helsinki.moodi.interceptor.AuthorizingInterceptor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${httpClient.connectTimeout} milliseconds")
    private Timeout connectTimeout;

    @Value("${httpClient.socketTimeout} milliseconds")
    private Timeout socketTimeout;

    @Autowired
    private AuthorizingInterceptor authorizingInterceptor;

    @Autowired
    private AccessLoggingInterceptor accessLoggingInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(accessLoggingInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(authorizingInterceptor).excludePathPatterns("/login", "/logout");
    }

    @Bean
    HttpClientBuilder httpClientBuilder() {
        ConnectionReuseStrategy noConnectionReuseStrategy = new ConnectionReuseStrategy() {
            @Override
            public boolean keepAlive(HttpRequest request, HttpResponse response, HttpContext context) {
                return false;
            }
        };

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            // .setSocketTimeout(socketTimeout)
            .build();
        // Socket timeout until the HTTPS connection has been established:
        SocketConfig socketConfig = SocketConfig.custom()
            .setSoTimeout(socketTimeout)
            .build();
        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            // .setDefaultSocketConfig(socketConfig)
            .setConnectionReuseStrategy(noConnectionReuseStrategy);
    }
}
