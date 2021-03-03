// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import static com.meterware.simplestub.Stub.createStrictStub;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;

/**
 * @author Russell Gold
 */
public abstract class HttpServletResponseStub implements HttpServletResponse {
    private int status = SC_OK;
    private final ServletOutputStreamStub out = createStrictStub(ServletOutputStreamStub.class);
    private final Map<String,List<String>> headers = new HashMap<>();
    private boolean responseSent = false;

    public static HttpServletResponseStub createServletResponse() {
        return createStrictStub(HttpServletResponseStub.class);
    }

    public String getHtml() {
        return out.html;
    }

    public String getRedirectLocation() {
        return getHeaders("Location").stream().findFirst().orElse(null);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return out;
    }

    @Override
    public void sendError(int sc, String msg) {
        status = sc;
        responseSent = true;
        out.html = msg;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int sc) {
        status = sc;
    }

    @Override
    public void sendRedirect(String location) {
        setHeader("Location", location);
        sendError(HTTP_MOVED_TEMP, "Found");
    }

    @Override
    public void setHeader(String name, String value) {
        if (responseSent) throw new IllegalStateException("Response already committed");

        headers.put(name, Collections.singletonList(value));
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    abstract static class ServletOutputStreamStub extends ServletOutputStream {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private String html;

        @Override
        public void write(int b) {
            baos.write(b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            html = baos.toString("UTF-8");
        }
    }
}
