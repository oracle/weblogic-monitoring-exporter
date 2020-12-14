// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;

import static com.oracle.wls.exporter.ServletConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.ServletConstants.CONTENT_TYPE_HEADER;
import static com.oracle.wls.exporter.ServletConstants.COOKIE_HEADER;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * A client for sending http requests.  Note that it does not do any cookie management or authentication by itself.
 *
 * @author Russell Gold
 */
abstract class WebClientCommon implements WebClient {

    private String setCookieHeader;
    private String authentication;
    private String sessionCookie;
    private boolean retryNeeded;
    private String contentType;
    private String url;

    interface WebRequest {
        String getMethod();
        URI getURI();
    }

    interface WebResponse extends Closeable {
        InputStream getContents();
        int getResponseCode();
        Stream<String> getHeadersAsStream(String headerName);
    }

    interface HttpClientExec extends Closeable {
        WebResponse send(WebRequest request) throws IOException;
    }

    /**
     * Creates an instance for communications with a specific URL.
     * @param url a URL for requests
     */
    @Override
    public WebClientCommon withUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Session headers are those which are computed by the web client, and which change based on session information,
     * such as authentic credentials and cookies. This method clears any currently defined.
     */
    abstract void clearSessionHeaders();

    /**
     * This method defines a header computed by the web client.
     * @param key header name
     * @param value header value
     */
    abstract void putSessionHeader(String key, String value);

    /**
     * Creates an object which can send a WebRequest and return a WebResponse. It will send
     * both headers defined by #addHeader and session header.
     * @throws GeneralSecurityException if unable to create the object
     */
    abstract HttpClientExec createClientExec() throws GeneralSecurityException;

    /**
     * Creates a GET request for the specified URL
     * @param url the URL to which the request should be sent
     */
    abstract WebRequest createGetRequest(String url);

    /**
     * Creates a POST requested for the specified URL and body
     * @param url the URL to which the request should be sent
     * @param postBody the body to send in the request
     */
    abstract WebRequest createPostRequest(String url, String postBody);

    /**
     * Creates a PUT requested for the specified URL and body
     * @param url the URL to which the request should be sent
     * @param putBody the body to send in the request
     */
    abstract WebRequest createPutRequest(String url, String putBody);

    protected String getContentType() {
        return contentType;
    }

    final String getUrl() {
        return url;
    }

    @Override
    public String doGetRequest() throws IOException {
        defineSessionHeaders();
        return sendRequest(createGetRequest(url));
    }

    @Override
    public String doPostRequest(String postBody) throws IOException {
        if (contentType == null) contentType = APPLICATION_JSON;
        defineSessionHeaders();
        return sendRequest(createPostRequest(url, postBody));
    }

    @Override
    public String doPutRequest(String putBody) throws IOException {
        defineSessionHeaders();
        return sendRequest(createPutRequest(url, putBody));
    }

    // Sends the specified request to the server
    private String sendRequest(WebRequest request) throws IOException {
        try (HttpClientExec clientExec = createClientExec()) {
            return getReply(clientExec.send(request));
        } catch (UnknownHostException | ConnectException e) {
            throw new RestPortConnectionException(request.getURI().toString());
        } catch (GeneralSecurityException e) {
            throw new WebClientException(e, "Unable to execute %s request to %s", request.getMethod(), request.getURI());
        }
    }

    private String getReply(WebResponse response) throws IOException {
        processStatusCode(response);
        try (final InputStream contents = response.getContents()) {
            return toString(contents);
        }
    }

    private void processStatusCode(WebResponse response) {
        switch (response.getResponseCode()) {
            case SC_BAD_REQUEST:
                throw new RestQueryException();
            case SC_UNAUTHORIZED:
                throw createAuthenticationChallengeException(response);
            case SC_FORBIDDEN:
                throw new ForbiddenException();
            case SC_OK:
                String setCookieHeader = extractSessionSetCookieHeader(response);
                if (setCookieHeader != null) {
                    this.setCookieHeader = setCookieHeader;
                    setSessionCookie(extractSessionCookie(setCookieHeader));
                }
            default:
                if (response.getResponseCode() > SC_BAD_REQUEST)
                    throw new ServerErrorException(response.getResponseCode());
        }
    }

    // Converts an input stream to a string.
    private String toString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int numBytes = 0;
        while (numBytes >= 0) {
            baos.write(buffer, 0, numBytes);
            numBytes = inputStream.read(buffer);
        }
        return baos.toString("UTF8");
    }

    final void defineSessionHeaders() {
        clearSessionHeaders();
        if (getAuthentication() != null) putSessionHeader(AUTHENTICATION_HEADER, getAuthentication());
        if (getSessionCookie() != null) putSessionHeader(COOKIE_HEADER, getSessionCookie());
        if (getContentType() != null) putSessionHeader(CONTENT_TYPE_HEADER, getContentType());
    }

    private AuthenticationChallengeException createAuthenticationChallengeException(WebResponse response) {
        return new AuthenticationChallengeException(getAuthenticationHeader(response));
    }

    private String getAuthenticationHeader(WebResponse response) {
        return response.getHeadersAsStream("WWW-Authenticate").filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String extractSessionSetCookieHeader(WebResponse response) {
        return response.getHeadersAsStream("Set-Cookie").filter(this::isSessionCookie).findFirst().orElse(null);
    }

    private boolean isSessionCookie(String headerValue) {
        return ExporterSession.getSessionCookie(headerValue) != null;
    }

    String extractSessionCookie(String setCookieHeaderValue) {
        return ExporterSession.getSessionCookie(setCookieHeaderValue);
    }

    static class EmptyInputStream extends InputStream {
        @Override
        public int read() {
            return -1;
        }
    }

    /**
     * Defines the authentication header to be sent on every request.
     * @param authentication the requester authentication information
     */
    @Override
    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    /**
     * Returns the authentication header to be sent on every request.
     * @return an authentication string
     */
    @Override
    public String getAuthentication() {
        return authentication;
    }

    /**
     * Defines the fixed session cookie to be used by the client.
     * @param sessionCookie a cookie representing the session on the REST API
     */
    @Override
    public void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    /**
     * Returns the cookie representing the session on the REST API
     * @return a string consisting of the name and value of a session cookie
     */
    @Override
    public String getSessionCookie() {
        return sessionCookie;
    }

    @Override
    public String getSetCookieHeader() {
        return setCookieHeader;
    }

    /**
     * Adds relevant headers to the response for the client
     * @param resp the response returned to the client
     */
    @Override
    public void forwardResponseHeaders(HttpServletResponse resp) {
        if (getSetCookieHeader() != null) {
            resp.setHeader("Set-Cookie", getSetCookieHeader());
            cacheSessionCookie();
        }
    }

    // Records the session information for future REST requests
    private void cacheSessionCookie() {
        ExporterSession.cacheSession(authentication, getSessionCookie());
    }

    /**
     * Marks the web client as needing to retry. This is typically done when unable to access a specified port.
     */
    @Override
    public void setRetryNeeded() {
        this.retryNeeded = true;
    }

    void clearRetryNeeded() {
        retryNeeded = false;
    }

    /**
     * Returns true if the 'retry needed' flag was set. Resets the flag on exit.
     * @return true if a retry was requested
     */
    @Override
    public boolean isRetryNeeded() {
        try {
            return retryNeeded;
        } finally {
            retryNeeded = false;
        }
    }
}
