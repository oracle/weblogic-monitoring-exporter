package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static com.meterware.simplestub.Stub.createStrictStub;

/**
 * @author Russell Gold
 */
@SuppressWarnings("SameParameterValue")
abstract class HttpServletRequestStub implements HttpServletRequest {
    final static String HOST = "myhost";
    final static int PORT = 7654;

    private final static String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private Map<String,String> headers = new HashMap<>();
    private String method;
    private String contentType = DEFAULT_CONTENT_TYPE;
    private String contents;
    private ServletInputStream inputStream;
    private String contextPath;
    private String servletPath = "";
    private HttpSessionStub session;

    static HttpServletRequestStub createGetRequest() {
        return createStrictStub(HttpServletRequestStub.class, "GET");
    }

    static HttpServletRequestStub createPostRequest() {
        return createStrictStub(HttpServletRequestStub.class, "POST");
    }

    HttpServletRequestStub(String method) {
        this.method = method;
    }

    void setHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
    }

    void setMultipartContent(String contents, String boundary) {
        this.contentType = "multipart/form-data; boundary=" + boundary;
        this.contents = contents;
    }

    void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        final String header = getHeader(name);
        return header == null ? new Vector<String>(Collections.emptyList()).elements()
                              : new Vector<>(Collections.singletonList(header)).elements();
    }

    @Override
    public int getContentLength() {
        return contents == null ? 0 : contents.getBytes().length;
    }

    @Override
    public String getCharacterEncoding() {
        return Charset.defaultCharset().name();
    }

    @Override
    public String getServerName() {
        return HOST;
    }

    @Override
    public int getServerPort() {
        return PORT;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (inputStream == null)
            inputStream = createStrictStub(ExporterServletTest.ServletInputStreamStub.class, contents);
        return inputStream;
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (create && session == null)
            session = createStrictStub(HttpSessionStub.class);
        return session;
    }

    boolean hasInvalidatedSession() {
        return session != null && !session.valid;
    }

    static abstract class HttpSessionStub implements HttpSession {
        private boolean valid = true;

        @Override
        public void invalidate() {
            valid = false;
        }
    }
}
