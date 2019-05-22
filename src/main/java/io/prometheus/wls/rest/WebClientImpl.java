package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.prometheus.wls.rest.ServletConstants.AUTHENTICATION_HEADER;
import static io.prometheus.wls.rest.ServletConstants.COOKIE_HEADER;
import static javax.servlet.http.HttpServletResponse.*;

/**
 * A production implementation of the web client interface that uses Apache HttpClient code.
 *
 * @author Russell Gold
 */
public class WebClientImpl extends WebClient {
    private static final char QUOTE = '"';

    private String url;
    private List<BasicHeader> addedHeaders = new ArrayList<>();
    private List<BasicHeader> sessionHeaders = new ArrayList<>();
    private String setCookieHeader;

    @Override
    WebClient withUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    void addHeader(String name, String value) {
        addedHeaders.add(new BasicHeader(name, value));
    }

    @Override
    String doGetRequest() throws IOException {
        addSessionHeaders();
        return sendRequest(new HttpGet(url));
    }

    private String sendRequest(HttpRequestBase request) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            return getReply(httpClient, request);
        } catch (UnknownHostException | ConnectException e) {
            throw new WebClientException(e, "Unable to execute %s request to %s", request.getMethod(), request.getURI());
        }
    }

    private String getReply(CloseableHttpClient httpClient, HttpRequestBase request) throws IOException {
        CloseableHttpResponse response = httpClient.execute(request);
        processStatusCode(response);
        return response.getEntity() == null ? null : getContentAsString(response.getEntity());
    }

    private String getContentAsString(HttpEntity responseEntity) throws IOException {
        try (InputStream inputStream = responseEntity.getContent()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int numBytes = 0;
            while (numBytes >= 0) {
                baos.write(buffer, 0, numBytes);
                numBytes = inputStream.read(buffer);
            }
            return baos.toString("UTF-8");
        }
    }

    @Override
    public String doPostRequest(String postBody) throws IOException {
        addSessionHeaders();
        return sendRequest(createPostRequest(postBody));
    }

    private HttpPost createPostRequest(String postBody) {
        HttpPost query = new HttpPost(url);
        query.setEntity(new StringEntity(postBody, ContentType.APPLICATION_JSON));
        return query;
    }

    private void addSessionHeaders() {
        sessionHeaders.clear();
        if (getAuthentication() != null) putSessionHeader(AUTHENTICATION_HEADER, getAuthentication());
        if (getSessionCookie() != null) putSessionHeader(COOKIE_HEADER, getSessionCookie());
    }

    private void putSessionHeader(String key, String value) {
        sessionHeaders.add(new BasicHeader(key, value));
    }

    @Override
    String doPutRequest(String putBody) throws IOException {
        addSessionHeaders();
        return sendRequest(createPutRequest(putBody));
    }

    private HttpPut createPutRequest(String putBody) {
        HttpPut query = new HttpPut(url);
        query.setEntity(new StringEntity(putBody, ContentType.APPLICATION_JSON));
        return query;
    }

    @Override
    public String getSetCookieHeader() {
        return setCookieHeader;
    }

    private void processStatusCode(CloseableHttpResponse response) throws RestQueryException {
        switch (response.getStatusLine().getStatusCode()) {
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
        }
    }

    private BasicAuthenticationChallengeException createAuthenticationChallengeException(CloseableHttpResponse response) {
        return new BasicAuthenticationChallengeException(getRealm(response));
    }

    private String getRealm(CloseableHttpResponse response) {
        Header header = response.getFirstHeader("WWW-Authenticate");
        return extractRealm(header == null ? "" : header.getValue());
    }

    // the value should be of the form <Basic realm="<realm-name>" and we want to extract the realm name
    private String extractRealm(String authenticationHeaderValue) {
        int start = authenticationHeaderValue.indexOf(QUOTE);
        int end = authenticationHeaderValue.indexOf(QUOTE, start+1);
        return start > 0 ? authenticationHeaderValue.substring(start+1, end) : "none";
    }

    private String extractSessionSetCookieHeader(CloseableHttpResponse response) {
        for (Header header : response.getHeaders("Set-Cookie")) {
            String sessionCookie = ExporterSession.getSessionCookie(header.getValue());
            if (sessionCookie != null) return header.getValue();
        }
        return null;
    }

    private String extractSessionCookie(String setCookieHeaderValue) {
        return ExporterSession.getSessionCookie(setCookieHeaderValue);
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .setDefaultHeaders(getDefaultHeaders())
                .build();
    }

    private Collection<? extends Header> getDefaultHeaders() {
        List<Header> headers = new ArrayList<>(addedHeaders);
        headers.addAll(sessionHeaders);
        return headers;
    }
}
