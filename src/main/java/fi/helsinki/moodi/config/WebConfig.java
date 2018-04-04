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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthorizingInterceptor authorizingInterceptor;

    @Autowired
    private AccessLoggingInterceptor accessLoggingInterceptor;


    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(accessLoggingInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(authorizingInterceptor).excludePathPatterns("/login", "/logout");
    }
}
