// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class WebClientImpl extends WebClientCommon {

  static class Header {

    private final String name;
    private final String value;

    Header(String name, String value) {
      this.name = name;
      this.value = value;
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

  class Java11HttpClientExec implements HttpClientExec {

    private final HttpClient httpClient
          = HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_2)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .proxy(ProxySelector.getDefault())
          .build();

    @Override
    public WebResponse send(WebRequest request) throws IOException {
      try {
        Java11WebRequest webRequest = (Java11WebRequest) request;
        return new Java11WebResponse(httpClient.send(webRequest.getRequest(), BodyHandlers.ofInputStream()));
      } catch (ConnectException e) {
        throw new RestPortConnectionException(request.getURI().toString());
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }

    @Override
    public void close() {
    }
  }

  @Override
  HttpClientExec createClientExec() {
    return new Java11HttpClientExec();
  }

  @Override
  public List<MultipartItem> parse(HttpServletRequest request) throws ServletException {
    return MultipartContentParser.parse(request);
  }

  class Java11WebRequest implements WebRequest {

    private final HttpRequest request;

    Java11WebRequest(Function<HttpRequest.Builder, HttpRequest.Builder> requestType) {
      final HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(getUrl()));
      defaultHeaders.forEach(h -> builder.header(h.name, h.value));
      sessionHeaders.forEach(h -> builder.header(h.name, h.value));
      request = requestType.apply(builder).build();
    }

    HttpRequest getRequest() {
      return request;
    }

    @Override
    public String getMethod() {
      return request.method();
    }

    @Override
    public URI getURI() {
      return request.uri();
    }
  }

  static class Java11WebResponse implements WebResponse {

    private final HttpResponse<InputStream> httpResponse;

    Java11WebResponse(HttpResponse<InputStream> httpResponse) {
      this.httpResponse = httpResponse;
    }

    @Override
    public InputStream getContents() {
      return httpResponse.body();
    }

    @Override
    public int getResponseCode() {
      return httpResponse.statusCode();
    }

    @Override
    public Stream<String> getHeadersAsStream(String headerName) {
      return httpResponse.headers().allValues(headerName).stream();
    }

    @Override
    public void close() {
    }
  }

  @Override
  WebRequest createGetRequest(String url) {
    return new Java11WebRequest(HttpRequest.Builder::GET);
  }

  @Override
  WebRequest createPostRequest(String url, String postBody) {
    return new Java11WebRequest(b -> b.POST(HttpRequest.BodyPublishers.ofString(postBody)));
  }

  @Override
  WebRequest createPutRequest(String url, String putBody) {
    return new Java11WebRequest(b -> b.PUT(HttpRequest.BodyPublishers.ofString(putBody)));
  }

  @Override
  public void addHeader(String name, String value) {
    defaultHeaders.add(new Header(name, value));
  }
}
