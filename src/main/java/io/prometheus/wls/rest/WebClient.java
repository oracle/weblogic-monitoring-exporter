package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2020, Oracle Corporation and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import java.io.IOException;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;

/**
 * A client for sending http requests.
 *
 * @author Russell Gold
 */
abstract class WebClient {

    private String authentication;
    private String sessionCookie;
    private boolean retryNeeded;

    /**
     * Defines the authentication header to be sent on every request.
     * @param authentication the requestor authentication information
     */
    void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    /**
     * Returns the authentication header to be sent on every request.
     * @return an authentication string
     */
    String getAuthentication() {
        return authentication;
    }

    /**
     * Defines the fixed session cookie to be used by the client.
     * @param sessionCookie a cookie representing the session on the REST API
     */
    void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    /**
     * Returns the cookie representing the session on the REST API
     * @return a string consisting of the name and value of a session cookie
     */
    String getSessionCookie() {
        return sessionCookie;
    }

    /**
     * Establish the session headers to be used for all requests
     * @param authentication the client's authentication information
     * @param sessionCookie the session cookie from the client, if any.
     */
    void establishSession(String authentication, String sessionCookie) {
        setAuthentication(authentication);
        if (sessionCookie != null)
            setSessionCookie(sessionCookie);
        else if (Objects.equals(getAuthentication(), ExporterSession.getAuthentication()))
            setSessionCookie(ExporterSession.getSessionCookie());
    }

    /**
     * Adds relevant headers to the response for the client
     * @param resp the response returned to the client
     */
    void forwardResponseHeaders(HttpServletResponse resp) {
        if (getSetCookieHeader() != null) {
            resp.setHeader("Set-Cookie", getSetCookieHeader());
            cacheSessionCookie();
        }
    }

    void setRetryNeeded(boolean retryNeeded) {
        this.retryNeeded = retryNeeded;
    }

    /**
     * Returns true of the 'retry needed' flag was set. Rests the flag on exit.
     * @return true if a retry was requested
     */
    boolean isRetryNeeded() {
        try {
            return retryNeeded;
        } finally {
            retryNeeded = false;
        }
    }

    /**
     * Records the session information for future REST requests
     */
    private void cacheSessionCookie() {
        ExporterSession.cacheSession(authentication, getSessionCookie());
    }

    /**
     * Sets the url to which this client will send requests.
     * @param url a URL for requests
     */
    abstract WebClient withUrl(String url);

    /**
     * Adds a header to be sent on every query.
     * @param name the header name
     * @param value the header value
     */
    abstract void addHeader(String name, String value);

    /**
     * Sends a plain GET request to the defined URL without parameters
     * @return the body of the response
     */
    abstract String doGetRequest() throws IOException;

    /**
     * Sends a POST query to the server and returns the reply.
     * @param postBody query data
     * @return the body of the response
     */
    abstract String doPostRequest(String postBody) throws IOException;

    /**
     * Sends a PUT query to the server and returns the reply.
     * @param putBody query data
     * @return the body of the response
     */
    abstract String doPutRequest(String putBody) throws IOException;

    /**
     * Returns the set-cookie header received from the server, if any
     */
    abstract String getSetCookieHeader();
}
