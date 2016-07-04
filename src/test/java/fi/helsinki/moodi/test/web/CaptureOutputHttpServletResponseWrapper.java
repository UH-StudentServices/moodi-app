package fi.helsinki.moodi.test.web;

import org.springframework.mock.web.DelegatingServletOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

class CaptureOutputHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private PrintWriter writer;
    private boolean getOutputStreamCalled;
    private boolean getWriterCalled;
    private CharArrayWriter charWriter;
    private ServletOutputStream outputStream;

    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    public CaptureOutputHttpServletResponseWrapper(final HttpServletResponse response) {
        super(response);
        charWriter = new CharArrayWriter();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream != null) {
            return outputStream;
        }
        if (getWriterCalled) {
            throw new IllegalStateException("getWriter already called");
        }

        getOutputStreamCalled = true;

        outputStream = new DelegatingServletOutputStream(this.content);
        return outputStream;
    }

    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return writer;
        }
        if (getOutputStreamCalled) {
            throw new IllegalStateException("getOutputStream already called");
        }
        getWriterCalled = true;
        writer = new PrintWriter(charWriter);
        return writer;
    }

    @Override
    public String toString() {
        if (writer != null) {
            return charWriter.toString();
        } else if (outputStream != null) {
            return content.toString();
        } else {
            return null;
        }
    }
}