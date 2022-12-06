// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.meterware.simplestub.Stub.createStrictStub;

abstract class InvocationContextStub implements InvocationContext {

  static final String HOST_NAME = "myhost";
  static final int PORT = 7123;
  static final int REST_PORT = 7431;
  private final ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

  private final String authenticationHeader = null;
  private String contentType = "text/plain";
  private String redirectLocation = null;
  private InputStream requestStream = null;
  private int responseStatus = 0;
  private boolean secure;
  private final Map<String, List<String>> responseHeaders = new HashMap<>();
  private final Map<String, String> cookies = new HashMap<>();

  static InvocationContextStub create() {
    return createStrictStub(InvocationContextStub.class);
  }

  InvocationContextStub withHttps() {
    secure = true;
    return this;
  }

  InvocationContextStub withConfigurationForm(String effect, String configuration) throws IOException {
    contentType = MultipartTestUtils.getContentType();
    requestStream = new ByteArrayInputStream(MultipartTestUtils.createEncodedForm(effect, configuration).getBytes());
    return this;
  }

  InvocationContextStub withConfiguration(String contentType, String configuration) {
    this.contentType = contentType;
    this.requestStream = new ByteArrayInputStream(configuration.getBytes());
    return this;
  }

  void addCookie(String name, String value) {
    cookies.put(name, value);
  }

  String getRedirectLocation() {
    return redirectLocation;
  }

  String getResponse() {
    return responseStream.toString();
  }

  @SuppressWarnings("SameParameterValue")
  String getResponseHeader(String name) {
    return Optional.ofNullable(responseHeaders.get(name)).map(h-> h.get(0)).orElse(null);
  }

  List<String> getResponseHeaders(String name) {
    return Optional.ofNullable(responseHeaders.get(name)).orElse(Collections.emptyList());
  }

  int getResponseStatus() {
    return responseStatus;
  }

  @Override
  public void close() {
  }

  @Override
  public UrlBuilder createUrlBuilder() {
    return UrlBuilder.create(secure).withHostName(HOST_NAME).withPort(PORT);
  }

  @Override
  public String getApplicationContext() {
    return "/unitTest/";
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public String getAuthenticationHeader() {
    return authenticationHeader;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public Map<String, String> getCookies() {
    return cookies;
  }

  @Override
  public String getInstanceName() {
    return "unit test";
  }

  @Override
  public InputStream getRequestStream() {
    return requestStream;
  }

  @Override
  public void sendError(int status, String msg) {
    responseStatus = status;
  }

  @Override
  public void sendRedirect(String location) {
    redirectLocation = location;
  }

  @Override
  public void addResponseHeader(String name, String value) {
    responseHeaders.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
  }

  @Override
  public void setStatus(int status) {
    responseStatus = status;
  }

  @Override
  public PrintStream getResponseStream() {
    return new PrintStream(responseStream);
  }
}
