// Copyright 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
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

/**
 * A production implementation of the web client interface that uses Apache HttpClient code. Since it is intended
 * to invoke the WebLogic REST API from a web application on the same server, it does not do any enforcement
 * of signed certificates when using https.
 *
 * @author Russell Gold
 */
public class WebClientImpl extends WebClientCommon {

    private final List<BasicHeader> addedHeaders = new ArrayList<>();
    private final List<BasicHeader> sessionHeaders = new ArrayList<>();

    @Override
    public void addHeader(String name, String value) {
        addedHeaders.add(new BasicHeader(name, value));
    }

    @Override
    protected HttpGetRequest createGetRequest(String url) {
        return new HttpGetRequest(url);
    }

    static class HttpGetRequest extends HttpGet implements WebRequest {
        public HttpGetRequest(String uri) {
            super(uri);
        }
    }

    @Override
    protected WebRequest createPostRequest(String url, String postBody) {
        HttpPostRequest query = new HttpPostRequest(url);
        query.setEntity(new StringEntity(postBody, ContentType.APPLICATION_JSON));
        return query;
    }

    static class HttpPostRequest extends HttpPost implements WebRequest {
        public HttpPostRequest(String uri) {
            super(uri);
        }
    }

    @Override
    protected WebRequest createPutRequest(String url, String putBody) {
        HttpPutRequest query = new HttpPutRequest(url);
        query.setEntity(new StringEntity(putBody, ContentType.APPLICATION_JSON));
        return query;
    }

    static class HttpPutRequest extends HttpPut implements WebRequest {
        public HttpPutRequest(String uri) {
            super(uri);
        }
    }

    static class HttpResponseImpl implements WebResponse {
        private final CloseableHttpResponse response;

        public HttpResponseImpl(CloseableHttpResponse response) {
            this.response = response;
        }

        @Override
        public InputStream getContents() {
            return Optional.ofNullable(response.getEntity())
                  .map(this::getContentsAsStream)
                  .orElse(new EmptyInputStream());
        }

        private InputStream getContentsAsStream(HttpEntity entity) {
            try {
                return entity.getContent();
            } catch (IOException e) {
                throw new WebClientException("Unable to retrieve response contents");
            }
        }

        @Override
        public int getResponseCode() {
            return response.getStatusLine().getStatusCode();
        }

        @Override
        public Stream<String> getHeadersAsStream(String headerName) {
            return Arrays.stream(response.getHeaders(headerName)).map(Header::getValue);
        }

        @Override
        public void close() throws IOException {
            response.close();
        }
    }

    @Override
    HttpClientExec createClientExec() throws GeneralSecurityException {
        return new ApacheHttpClient();
    }

    class ApacheHttpClient implements HttpClientExec {
        private final CloseableHttpClient client;
        public ApacheHttpClient() throws GeneralSecurityException {
            SelfSignedCertificateAcceptor acceptor = new SelfSignedCertificateAcceptor();
            client = HttpClientBuilder.create()
                  .setDefaultHeaders(getDefaultHeaders())
                  .setSSLSocketFactory(acceptor.getSslConnectionSocketFactory())
                  .setConnectionManager(acceptor.getConnectionManager())
                  .build();
        }

        @Override
        public WebResponse send(WebRequest request) throws IOException {
            try {
                return new HttpResponseImpl(client.execute((HttpUriRequest) request));
            } catch (HttpHostConnectException e) {
                throw new RestPortConnectionException(e.getHost().toURI());
            }
        }

        @Override
        public void close() throws IOException {
            client.close();
        }
    }

    @Override
    void clearSessionHeaders() {
        sessionHeaders.clear();
    }

    @Override
    void putSessionHeader(String key, String value) {
        sessionHeaders.add(new BasicHeader(key, value));
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
