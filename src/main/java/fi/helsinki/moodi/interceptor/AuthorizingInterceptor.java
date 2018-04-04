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

package fi.helsinki.moodi.interceptor;

import fi.helsinki.moodi.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthorizingInterceptor extends HandlerInterceptorAdapter {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_TOKEN = "client-token";
    private static final String CLIENT = "client";
    private final Environment environment;

    @Autowired
    public AuthorizingInterceptor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler) throws Exception {

        if (isAuthorizationDisabled()) {
            return true;
        }

        if (authorizeFromCookie(request)) {
            return true;
        }

        if (authorizeFromParameters(request)) {
            return true;
        }

        if (authorizeFromHeaders(request)) {
            return true;
        }

        throw new UnauthorizedException("Unauthorized");
    }

    private boolean authorizeFromHeaders(final HttpServletRequest request) {
        return authorize(
                request.getHeader(CLIENT_ID),
                request.getHeader(CLIENT_TOKEN));
    }

    private boolean authorizeFromParameters(final HttpServletRequest request) {
        return authorize(
                request.getParameter(CLIENT_ID),
                request.getParameter(CLIENT_TOKEN));
    }

    private boolean authorizeFromCookie(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        for (final Cookie cookie : cookies) {
            if (CLIENT.equals(cookie.getName())) {
                final String value = cookie.getValue();
                if (value != null) {
                    final String[] parts = value.split(":");
                    return authorize(parts[0], parts[1]);
                }
            }
        }

        return false;
    }

    private boolean isAuthorizationDisabled() {
        return !environment.getProperty("auth.enabled", Boolean.class, true);
    }

    private boolean authorize(final String clientId, final String clientToken) {
        return clientId != null &&
                clientToken != null &&
                clientToken.equals(environment.getProperty("auth.client." + clientId));
    }
}
