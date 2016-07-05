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