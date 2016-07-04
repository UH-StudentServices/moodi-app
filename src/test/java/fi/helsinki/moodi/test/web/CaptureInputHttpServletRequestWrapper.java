package fi.helsinki.moodi.test.web;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

public final class CaptureInputHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final String requestBody;

    public CaptureInputHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);

        // read the original payload into the xmlPayload variable
        final StringBuilder stringBuilder = new StringBuilder();

        try {
            final InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                    char[] charBuffer = new char[128];
                    int bytesRead = -1;
                    while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                        stringBuilder.append(charBuffer, 0, bytesRead);
                    }
                }
            } else {
                stringBuilder.append("");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading the request payload", e);
        }

        requestBody = stringBuilder.toString();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody.getBytes());
        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    public String getRequestBodyAsString() {
        return requestBody;
    }
}