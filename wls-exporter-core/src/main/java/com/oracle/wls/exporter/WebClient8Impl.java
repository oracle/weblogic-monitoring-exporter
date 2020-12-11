// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A stripped-down web client that uses classes built into Java 1.8.
 */
public class WebClient8Impl extends WebClientCommon {

  static class Header {
    private final String name;
    private final String value;

    Header(String name, String value) {
      this.name = name;
      this.value = value;
    }

    void addHeader(HttpURLConnection connection) {
      connection.setRequestProperty(name, value);
    }
  }

  private final List<Header> defaultHeaders = new ArrayList<>();
  private final List<Header> sessionHeaders = new ArrayList<>();

  @Override
  void clearSessionHeaders() {
    sessionHeaders.clear();
  }

  @Override
  void putSessionHeader(String key, String value) {
    sessionHeaders.add(new Header(key, value));
  }

  @Override
  HttpClientExec createClientExec() {
    return new Java8HttpClientExec();
  }

  @Override
  WebRequest createGetRequest(String url) {
    return new Java8WebRequest("GET", url);
  }

  @Override
  WebRequest createPostRequest(String url, String postBody) {
    return new Java8WebRequestWithBody("POST", url, postBody);
  }

  @Override
  WebRequest createPutRequest(String url, String putBody) {
    return new Java8WebRequestWithBody("PUT", url, putBody);
  }

  @Override
  public void addHeader(String name, String value) {
    defaultHeaders.add(new Header(name, value));
  }

  class Java8WebRequest implements WebRequest {
    private final String method;
    private final String url;

    Java8WebRequest(String method, String url) {
      this.method = method;
      this.url = url;
    }

    @Override
    public String getMethod() {
      return method;
    }

    @Override
    public URI getURI() {
      try {
        return new URI(url);
      } catch (URISyntaxException e) {
        throw new WebClientException(e, "Unable parse URL %1", url);
      }
    }

    public void completeRequest(HttpURLConnection connection) throws IOException {
      defaultHeaders.forEach(h -> h.addHeader(connection));
      sessionHeaders.forEach(h -> h.addHeader(connection));
      connection.setRequestMethod(method);
    }

  }

  class Java8WebRequestWithBody extends Java8WebRequest {
    private final String body;

    public Java8WebRequestWithBody(String method, String url, String body) {
      super(method, url);
      this.body = body;
    }

    @Override
    public void completeRequest(HttpURLConnection connection) throws IOException {
      super.completeRequest(connection);
      connection.setDoOutput(true);
      
      try (final OutputStream outputStream = connection.getOutputStream()) {
        outputStream.write(body.getBytes());
      }
    }
  }

  static class Java8WebResponse implements WebResponse {
    private final HttpURLConnection connection;
    private final int responseCode;
    private final Map<String, List<String>> headerFields;

    Java8WebResponse(HttpURLConnection connection) throws IOException {
      this.connection = connection;
      responseCode = connection.getResponseCode();
      headerFields = connection.getHeaderFields();
    }

    @Override
    public InputStream getContents() {
      try {
        return connection.getInputStream();
      } catch (IOException e) {
        return new EmptyInputStream();
      }
    }

    @Override
    public int getResponseCode() {
      return responseCode;
    }

    @Override
    public Stream<String> getHeadersAsStream(String headerName) {
      return Optional.ofNullable(headerFields.get(headerName)).map(Collection::stream).orElse(Stream.empty());
    }

    @Override
    public void close() {

    }
  }

  static class Java8HttpClientExec implements HttpClientExec {
    @Override
    public WebResponse send(WebRequest request) throws IOException {
      HttpURLConnection connection = openConnection(request.getURI().toURL());
      ((Java8WebRequest) request).completeRequest(connection);

      return new Java8WebResponse(connection);
    }

    /**
     * open a connection for the given uniform resource locator
     * @param url - the url to use
     */
    private HttpURLConnection openConnection(URL url ) throws IOException {
      try {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects( true );
        connection.setUseCaches( false );
        return connection;
      } catch (WebClientException e) {
        if (e.getCause() instanceof IOException)
          throw (IOException) e.getCause();
        else
          throw e;
      }
    }

    @Override
    public void close() {

    }
  }
}
