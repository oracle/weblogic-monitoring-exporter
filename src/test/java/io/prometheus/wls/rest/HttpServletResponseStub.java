package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.meterware.simplestub.Stub.createStrictStub;

/**
 * @author Russell Gold
 */
abstract class HttpServletResponseStub implements HttpServletResponse {
    private int status = SC_OK;
    private ServletOutputStreamStub out = createStrictStub(ServletOutputStreamStub.class);
    private Map<String,List<String>> headers = new HashMap<>();
    private boolean responseSent = false;
    private String redirectLocation;

    static HttpServletResponseStub createServletResponse() {
        return createStrictStub(HttpServletResponseStub.class);
    }

    String getHtml() {
        return out.html;
    }

    String getRedirectLocation() {
        return redirectLocation;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return out;
    }

    @Override
    public void sendError(int sc, String msg) {
        status = sc;
        responseSent = true;
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
        redirectLocation = location;
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
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
