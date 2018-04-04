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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AccessLoggingInterceptor extends HandlerInterceptorAdapter {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_ID_COOKIE_NAME = "client";

    private final Logger logger = LoggerFactory.getLogger(AccessLoggingInterceptor.class);

    @Override
    public boolean preHandle(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Object handler) throws Exception {

        logger.info(String.format("Incoming %s request to Moodi api %s from %s with clientId %s",
            request.getMethod(),
            request.getRequestURI(),
            getRequestIp(request),
            getClientId(request)));

        return true;
    }

    private String getRequestIp(final HttpServletRequest request) {
        String requestIp = request.getHeader("X-FORWARDED-FOR");

        if(requestIp == null) {
            requestIp = request.getRequestURI();
        }

        return requestIp;
    }

    private String getClientId(HttpServletRequest request) {
        String clientId;

        clientId = getClientIdFromHeaders(request);

        if(clientId == null) {
            clientId = getClientIdFromParameters(request);
        }

        if(clientId == null) {
            clientId = getClientIdFromCookie(request);
        }

        return clientId;
    }

    private String getClientIdFromParameters(HttpServletRequest request) {
        return request.getParameter(CLIENT_ID);
    }

    private String getClientIdFromHeaders(HttpServletRequest request) {
        return request.getHeader(CLIENT_ID);
    }

    private String getClientIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if(cookies == null) {
            return null;
        }

        return Lists.newArrayList(cookies).stream()
            .filter(cookie -> CLIENT_ID_COOKIE_NAME.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(value -> value != null)
            .map(value -> value.split(":")[0])
            .findFirst().get();
    }
}
