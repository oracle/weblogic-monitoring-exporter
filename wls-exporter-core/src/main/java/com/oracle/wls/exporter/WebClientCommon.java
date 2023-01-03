// Copyright (c) 2017, 2023, Oracle and/or its affiliates.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_CHALLENGE_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.CONTENT_TYPE_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.SET_COOKIE_HEADER;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * A client for sending http requests.  Note that it does not do any authentication by itself.
 *
 * @author Russell Gold
 */
public abstract class WebClientCommon implements WebClient {

    private String authentication;
    private boolean retryNeeded;
    private String contentType;
    private String url;
    private final List<Consumer<String>> setCookieHandlers = new ArrayList<>();

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
     * such as authentic credentials. This method clears any currently defined.
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
     * Creates a PUT requested for the specified URL and body, formatted as JSON.
     * @param url the URL to which the request should be sent
     * @param putBody the body to send in the request
     */
    abstract <T> WebRequest createPutRequest(String url, T putBody);

    protected String getContentType() {
        return contentType;
    }

    @Override
    public String doGetRequest() throws IOException {
        defineSessionHeaders();
        return sendRequest(createGetRequest(url)).getBody();
    }

    @Override
    public String doPostRequest(String postBody) throws IOException {
        if (contentType == null) contentType = APPLICATION_JSON;
        defineSessionHeaders();
        return sendRequest(createPostRequest(url, postBody)).getBody();
    }

    @Override
    public <T> String doPutRequest(T putBody) throws IOException {
        defineSessionHeaders();
        return sendRequest(createPutRequest(url, putBody)).getBody();
    }

    // Sends the specified request to the server
    private ResponseImpl sendRequest(WebRequest request) throws IOException {
        try (HttpClientExec clientExec = createClientExec()) {
            return new ResponseImpl(clientExec.send(request));
        } catch (UnknownHostException | ConnectException e) {
            throw new RestPortConnectionException(request.getURI().toString());
        } catch (GeneralSecurityException e) {
            throw new WebClientException(e, "Unable to execute %s request to %s", request.getMethod(), request.getURI());
        }
    }

    final void defineSessionHeaders() {
        clearSessionHeaders();
        if (getAuthentication() != null) putSessionHeader(AUTHENTICATION_HEADER, getAuthentication());
        if (getContentType() != null) putSessionHeader(CONTENT_TYPE_HEADER, getContentType());
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
     * Marks the web client as needing to retry. This is typically done when unable to access a specified port.
     */
    @Override
    public void setRetryNeeded() {
        this.retryNeeded = true;
    }

    public void clearRetryNeeded() {
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

    @Override
    public void onSetCookieReceivedDo(Consumer<String> setCookieHandler) {
        setCookieHandlers.add(setCookieHandler);
    }

    protected void invokeSetCookieHandlerCallbacks(List<String> setCookieHeaders) {
        setCookieHandlers.forEach(setCookieHeaders::forEach);
    }

    class ResponseImpl implements WebClient.Response {
        private final WebResponse response;
        private final String body;

        ResponseImpl(WebResponse response) throws IOException {
            this.response = response;
            processStatusCode();
            reportSetCookieHeaders();

            try (final InputStream contents = response.getContents()) {
                body = asString(contents);
            }
        }

        private void processStatusCode() {
            switch (response.getResponseCode()) {
                case HTTP_BAD_REQUEST:
                    throw new RestQueryException();
                case HTTP_UNAUTHORIZED:
                    throw createAuthenticationChallengeException();
                case HTTP_FORBIDDEN:
                    throw new ForbiddenException();
                default:
                    if (response.getResponseCode() > SC_BAD_REQUEST)
                        throw createServerErrorException();
            }
        }

        private void reportSetCookieHeaders() {
            invokeSetCookieHandlerCallbacks(getSetCookieHeaders());
        }

        private List<String> getSetCookieHeaders() {
            return response.getHeadersAsStream(SET_COOKIE_HEADER).filter(Objects::nonNull).collect(Collectors.toList());
        }

        private ServerErrorException createServerErrorException() {
            try {
                return new ServerErrorException(response.getResponseCode(), asString(response.getContents()));
            } catch (IOException e) {
                return new ServerErrorException(response.getResponseCode());
            }
        }

        private AuthenticationChallengeException createAuthenticationChallengeException() {
            return new AuthenticationChallengeException(getAuthenticationHeader());
        }

        private String getAuthenticationHeader() {
            return response.getHeadersAsStream(AUTHENTICATION_CHALLENGE_HEADER).filter(Objects::nonNull).findFirst().orElse(null);
        }

        private String asString(InputStream inputStream) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int numBytes = 0;
            while (numBytes >= 0) {
                baos.write(buffer, 0, numBytes);
                numBytes = inputStream.read(buffer);
            }
            return baos.toString("UTF8");
        }

        @Override
        public String getBody() {
            return body;
        }

    }
}
