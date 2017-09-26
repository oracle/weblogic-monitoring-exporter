package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.prometheus.wls.rest.ServletConstants.*;

/**
 * @author Russell Gold
 */
public class WebClientImpl extends WebClient {
    private static final char QUOTE = '"';

    private String url;
    private List<BasicHeader> addedHeaders = new ArrayList<>();
    private String setCookieHeader;

    WebClientImpl(String url) {
        this.url = url;
    }

    @Override
    public String doQuery(String jsonQuery) throws IOException {
        addSessionHeaders();
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost query = new HttpPost(url);
            query.setEntity(new StringEntity(jsonQuery, ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(query);
            processStatusCode(response);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                try (InputStream inputStream = responseEntity.getContent()) {
                    byte[] buffer = new byte[4096];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int numBytes = 0;
                    while (numBytes >= 0) {
                        baos.write(buffer, 0, numBytes);
                        numBytes = inputStream.read(buffer);
                    }
                    return baos.toString("UTF-8");
                }
            }
        }
        return null;
    }

    private void addSessionHeaders() {
        addedHeaders.clear();
        if (getAuthentication() != null) putHeader(AUTHENTICATION_HEADER, getAuthentication());
        if (getSessionCookie() != null) putHeader(COOKIE_HEADER, getSessionCookie());
    }

    private void putHeader(String key, String value) {
        addedHeaders.add(new BasicHeader(key, value));
    }

    @Override
    public String getSetCookieHeader() {
        return setCookieHeader;
    }

    private void processStatusCode(CloseableHttpResponse response) throws RestQueryException {
        switch (response.getStatusLine().getStatusCode()) {
            case BAD_REQUEST:
                throw new RestQueryException();
            case AUTHENTICATION_REQUIRED:
                throw createAuthenticationChallengeException(response);
            case NOT_AUTHORIZED:
                throw new NotAuthorizedException();
            case SUCCESS:
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
        List<Header> headers = new ArrayList<>(Collections.singleton(new BasicHeader("X-Requested-By", "rest-exporter")));
        headers.addAll(addedHeaders);
        return headers;
    }
}
