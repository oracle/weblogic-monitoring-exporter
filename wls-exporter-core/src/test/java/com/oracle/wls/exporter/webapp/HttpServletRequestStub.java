// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.meterware.simplestub.Stub;

import static com.meterware.simplestub.Stub.createStrictStub;

/**
 * @author Russell Gold
 */
@SuppressWarnings("SameParameterValue")
public abstract class HttpServletRequestStub implements HttpServletRequest {
    public final static String HOST_NAME = "myhost";
    public final static int PORT = 7654;
    public final static int LOCAL_PORT = 7631;

    private final static String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private final Map<String,String> headers = new HashMap<>();
    private final String method;
    private String localhostName = "localhost";
    private String contentType = DEFAULT_CONTENT_TYPE;
    private String contents;
    private ServletInputStream inputStream;
    private String contextPath;
    private String servletPath = "";
    private HttpSessionStub session;
    private boolean secure;
    private String hostName = HOST_NAME;
    private int port = PORT;

    public static HttpServletRequestStub createGetRequest() {
        return createStrictStub(HttpServletRequestStub.class, "GET");
    }

    public static HttpServletRequestStub createPostRequest() {
        return createStrictStub(HttpServletRequestStub.class, "POST");
    }

    public HttpServletRequestStub withHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public HttpServletRequestStub withLocalHostName(String localhostName) {
        this.localhostName = localhostName;
        return this;
    }

    public HttpServletRequestStub withPort(int port) {
        this.port = port;
        return this;
    }

    HttpServletRequestStub(String method) {
        this.method = method;
    }

    public void setHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
    }

    public HttpServletRequestStub withMultipartContent(String contents, String boundary) {
        setContent("multipart/form-data; boundary=" + boundary, contents);
        return this;
    }

    void setContent(String contentType, String contents) {
        this.contentType = contentType;
        this.contents = contents;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
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
        return hostName;
    }

    @Override
    public int getServerPort() {
        return port;
    }

    @Override
    public String getLocalName() {
        return localhostName;
    }

    @Override
    public int getLocalPort() {
        return LOCAL_PORT;
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
    public ServletInputStream getInputStream() {
        if (inputStream == null)
            inputStream = Stub.createStrictStub(ServletInputStreamStub.class, contents);
        return inputStream;
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (create && session == null)
            session = createStrictStub(HttpSessionStub.class);
        return session;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    public boolean hasInvalidatedSession() {
        return session != null && !session.valid;
    }

    static abstract class HttpSessionStub implements HttpSession {
        private boolean valid = true;

        @Override
        public void invalidate() {
            valid = false;
        }
    }


    abstract static class ServletInputStreamStub extends ServletInputStream {
        private final InputStream inputStream;

        public ServletInputStreamStub(String contents) {
            inputStream = new ByteArrayInputStream(contents.getBytes());
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}
