package fi.helsinki.moodi.web;

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
