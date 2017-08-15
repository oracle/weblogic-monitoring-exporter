package io.prometheus.wls.rest;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.meterware.simplestub.Stub.createStrictStub;

abstract class HttpServletRequestStub implements HttpServletRequest {
    final static String HOST = "myhost";
    final static int PORT = 7654;

    private final static String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private Map<String,String> headers = new HashMap<>();
    private String method;
    private String contentType = DEFAULT_CONTENT_TYPE;
    private String contents;
    private ServletInputStream inputStream;

    static HttpServletRequestStub createGetRequest() {
        return createStrictStub(HttpServletRequestStub.class, "GET");
    }

    static HttpServletRequestStub createPostRequest() {
        return createStrictStub(HttpServletRequestStub.class, "POST");
    }

    HttpServletRequestStub(String method) {
        this.method = method;
    }

    @SuppressWarnings("SameParameterValue")
    void setHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
    }

    void setMultipartContent(String contents, String boundary) {
        this.contentType = "multipart/form-data; boundary=" + boundary;
        this.contents = contents;
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
    public ServletInputStream getInputStream() throws IOException {
        if (inputStream == null)
            inputStream = createStrictStub(ExporterServletTest.ServletInputStreamStub.class, contents);
        return inputStream;
    }
}
