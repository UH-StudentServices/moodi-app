package fi.helsinki.moodi.test.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.helsinki.moodi.util.TabularFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RequestLoggerFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggerFilter.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(
            final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final CaptureInputHttpServletRequestWrapper requestWrapper =
                new CaptureInputHttpServletRequestWrapper((HttpServletRequest) request);

        final CaptureOutputHttpServletResponseWrapper responseWrapper =
                new CaptureOutputHttpServletResponseWrapper((HttpServletResponse) response);

        final String url = requestWrapper.getRequestURI();
        final String method = requestWrapper.getMethod();

        logRequest(requestWrapper, url, method);

        chain.doFilter(requestWrapper, responseWrapper);

        final String responseString = responseWrapper.toString();
        logResponse(responseWrapper, url, method);

        if (responseString != null) {
            response.getOutputStream().write(responseString.getBytes());
        }

    }

    @Override
    public void destroy() {}

    private void logRequest(
            final CaptureInputHttpServletRequestWrapper request, final String url, final String method) {

        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        final StringBuilder sb = new StringBuilder();

        printRequestBegin(sb, url, method);
        printRequestHeaders(sb, request);
        printBody(sb, request.getRequestBodyAsString());
        printRequestEnd(sb, url, method);

        LOGGER.trace(sb.toString());
    }

    private void printRequestHeaders(
            final StringBuilder sb, final CaptureInputHttpServletRequestWrapper request) {
        printHeaders(sb, () -> Collections.list(request.getHeaderNames()), request::getHeader);
    }

    private void printRequestBegin(final StringBuilder sb, final String url, final String method) {
        sb.append(String.format("\n---------- request begin : %s : %s ----------\n", url, method));
    }

    private void printRequestEnd(final StringBuilder sb, final String url, final String method) {
        sb.append(String.format("\n---------- request end   : %s : %s ----------", url, method));
    }

    private void logResponse(
            final CaptureOutputHttpServletResponseWrapper response,
            final String url,
            final String method)
            throws IOException {

        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        final StringBuilder sb = new StringBuilder();

        printResponseBegin(sb, url, method);
        printResponseHeaders(sb, response);
        printBody(sb, response.toString());
        printResponseEnd(sb, url, method);

        LOGGER.trace(sb.toString());
    }

    private void printResponseHeaders(
            final StringBuilder sb, final CaptureOutputHttpServletResponseWrapper response) {
        printHeaders(sb, response::getHeaderNames, response::getHeader);
    }

    private void printHeaders(
            final StringBuilder sb,
            final Supplier<Collection<String>> headerNamesSupplier,
            final Function<String, String> headerValueSupplier) {

        final List<String> headers = new ArrayList<>();
        for (final String headerName : headerNamesSupplier.get()) {
            headers.add(headerName);
            headers.add(headerValueSupplier.apply(headerName));
        }

        printHeaders(sb, headers);
    }

    private void printHeaders(final StringBuilder sb, final List<String> headers) {
        sb.append("HEADERS:");

        if (headers.isEmpty()) {
            sb.append(" [no headers]\n");
        } else {
            final TabularFormat headersTable =
                    new TabularFormat(2, 0, headers.toArray(new String[headers.size()]));
            sb.append("\n").append(headersTable);
        }
    }

    private void printResponseEnd(final StringBuilder sb, final String url, final String method) {
        sb.append(String.format("\n---------- response end   : %s : %s ----------", url, method));
    }

    private void printResponseBegin(final StringBuilder sb, final String url, final String method) {
        sb.append(String.format("\n---------- response begin : %s : %s ----------\n", url, method));
    }

    private void printBody(final StringBuilder sb, final String body) {
        sb.append("\nBODY:");

        if (body == null || body.isEmpty()) {
            sb.append(" [empty body]");
        } else {
            sb.append("\n").append(prettyFormatJson(body));
        }
    }

    private String prettyFormatJson(final String output) {
        try {
            final Object json = objectMapper.readValue(output, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            LOGGER.warn("Could not pretty format json");
            return output;
        }
    }
}