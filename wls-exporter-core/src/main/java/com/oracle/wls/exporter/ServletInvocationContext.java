// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * An implementation for the InvocationContext for a servlet web application.
 */
public class ServletInvocationContext implements InvocationContext {

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final String localHostName = getLocalHostName();

  private Map<String,String> cookies;

  public ServletInvocationContext(HttpServletRequest request, HttpServletResponse response) {
    this.request = request;
    this.response = response;
  }

  @Override
  public void close() {
    Optional.ofNullable(request.getSession(false)).ifPresent(HttpSession::invalidate);
  }

  @Override
  public UrlBuilder createUrlBuilder() {
    return UrlBuilder.create(request.isSecure())
          .withHostName(request.getLocalName())
          .withHostName(localHostName)
          .withPort(LiveConfiguration.getConfiguredRestPort())
          .withPort(request.getLocalPort());
  }

  static String getLocalHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "localhost";
    }
  }

  @Override
  public String getApplicationContext() {
    return request.getContextPath();
  }

  @Override
  public String getAuthenticationHeader() {
      return request.getHeader(WebAppConstants.AUTHENTICATION_HEADER);
  }

  @Override
  public String getContentType() {
      return request.getContentType();
  }

  @Override
  public Map<String, String> getCookies() {
    if (cookies == null)
      cookies = getRequestCookies();
    return cookies;
  }

  private Map<String, String> getRequestCookies() {
    final Map<String, String> result = new HashMap<>();
    if (request.getCookies() != null)
      Arrays.stream(request.getCookies()).forEach(c -> result.put(c.getName(), c.getValue()));

    return result;
  }

  @Override
  public String getInstanceName() {
      return request.getServerName() + ":" + request.getServerPort();
  }

  @Override
  public InputStream getRequestStream() throws IOException {
      return request.getInputStream();
  }

  @Override
  public PrintStream getResponseStream() throws IOException {
      return new PrintStream(this.response.getOutputStream());
  }

  @Override
  public void sendError(int status, String msg) throws IOException {
    if (msg == null) {
      response.sendError(status);
    } else {
      response.sendError(status, msg);
    }
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    response.sendRedirect(location);
  }

  @Override
  public void addResponseHeader(String name, String value) {
      response.setHeader(name, value);
  }

  @Override
  public void setStatus(int status) {
      response.setStatus(status);
  }
}
