// Copyright 2017, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import static com.oracle.wls.exporter.ServletConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.ServletConstants.COOKIE_HEADER;
import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_GATEWAY_TIMEOUT;
import static javax.servlet.http.HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * A production implementation of the web client interface that uses Apache HttpClient code.
 *
 * @author Russell Gold
 */
public class WebClientImpl extends WebClient {
    private String url;
    private final List<BasicHeader> addedHeaders = new ArrayList<>();
    private final List<BasicHeader> sessionHeaders = new ArrayList<>();
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
        } catch (HttpHostConnectException e) {
            throw new RestPortConnectionException(e.getHost());
        } catch (UnknownHostException | ConnectException | GeneralSecurityException e) {
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
            case SC_INTERNAL_SERVER_ERROR:
            case SC_NOT_IMPLEMENTED:
            case SC_BAD_GATEWAY:
            case SC_SERVICE_UNAVAILABLE:
            case SC_GATEWAY_TIMEOUT:
            case SC_HTTP_VERSION_NOT_SUPPORTED:
                throw new ServerErrorException(response.getStatusLine().getStatusCode());
            case SC_OK:
                String setCookieHeader = extractSessionSetCookieHeader(response);
                if (setCookieHeader != null) {
                    this.setCookieHeader = setCookieHeader;
                    setSessionCookie(extractSessionCookie(setCookieHeader));
                }
        }
    }

    private AuthenticationChallengeException createAuthenticationChallengeException(CloseableHttpResponse response) {
        return new AuthenticationChallengeException(getAuthenticationHeader(response));
    }

    private String getAuthenticationHeader(CloseableHttpResponse response) {
        Header header = response.getFirstHeader("WWW-Authenticate");
        return Optional.ofNullable(header).map(Header::getValue).orElse("");
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

    private CloseableHttpClient createHttpClient() throws GeneralSecurityException {
        SelfSignedCertificateAcceptor acceptor = new SelfSignedCertificateAcceptor();
        return HttpClientBuilder.create()
                .setDefaultHeaders(getDefaultHeaders())
                .setSSLSocketFactory(acceptor.getSslConnectionSocketFactory())
                .setConnectionManager(acceptor.getConnectionManager())
                .build();
    }

    private Collection<? extends Header> getDefaultHeaders() {
        List<Header> headers = new ArrayList<>(addedHeaders);
        headers.addAll(sessionHeaders);
        return headers;
    }

    static class SelfSignedCertificateAcceptor {
        private final SSLConnectionSocketFactory sslConnectionSocketFactory;
        private final Registry<ConnectionSocketFactory> socketFactoryRegistry;

        SelfSignedCertificateAcceptor() throws GeneralSecurityException {
            sslConnectionSocketFactory = createSSLConnectionSocketFactory();
            socketFactoryRegistry = createSocketFactoryRegistry();
        }

        BasicHttpClientConnectionManager getConnectionManager() {
            return new BasicHttpClientConnectionManager(socketFactoryRegistry);
        }

        SSLConnectionSocketFactory getSslConnectionSocketFactory() {
            return sslConnectionSocketFactory;
        }

        private SSLConnectionSocketFactory createSSLConnectionSocketFactory() throws GeneralSecurityException {
            return new SSLConnectionSocketFactory(createSSLContext(), NoopHostnameVerifier.INSTANCE);
        }

        private SSLContext createSSLContext() throws GeneralSecurityException {
            return SSLContexts.custom()
                .loadTrustMaterial(null, createAcceptingTrustStrategy())
                .build();
        }

        private TrustStrategy createAcceptingTrustStrategy() {
            return (cert, authType) -> true;
        }

        private Registry<ConnectionSocketFactory> createSocketFactoryRegistry() {
            return RegistryBuilder.<ConnectionSocketFactory> create()
                  .register("http", new PlainConnectionSocketFactory())
                  .register("https", sslConnectionSocketFactory)
                  .build();
        }

    }
}
