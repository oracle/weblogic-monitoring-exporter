// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import com.oracle.wls.exporter.ServletInvocationContext;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.webapp.HttpServletRequestStub.HOST_NAME;
import static com.oracle.wls.exporter.webapp.HttpServletRequestStub.PORT;
import static com.oracle.wls.exporter.webapp.HttpServletResponseStub.SINGLE_ARG_METHOD_CALLED;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class ServletInvocationContextTest {

  private final HttpServletRequestStub request = HttpServletRequestStub.createPostRequest();
  private final HttpServletResponseStub response = HttpServletResponseStub.createServletResponse();
  private final ServletInvocationContext context = new ServletInvocationContext(request, response);

  @Test
  void afterClose_requestSessionIsInvalidated() {
    request.getSession(true);

    context.close();

    assertThat(request.hasInvalidatedSession(), is(true));
  }

  @Test
  void obtainAuthenticationHeader() {
    request.setHeader(AUTHENTICATION_HEADER, "A value");

    assertThat(context.getAuthenticationHeader(), equalTo("A value"));
  }

  @Test
  void obtainContentType() {
    request.setContent("text/plain", "Abcedef");

    assertThat(context.getContentType(), equalTo("text/plain"));
  }

  @Test
  void obtainInstanceName() {
    assertThat(context.getInstanceName(), equalTo(HOST_NAME + ":" + PORT));
  }

  @Test
  void dataFromClient_isReadableFromRequestStream() throws IOException {
    request.setContent("text/plain", "Abcedef");

    assertThat(new BufferedReader(new InputStreamReader(context.getRequestStream())).readLine(), equalTo("Abcedef"));
  }

  @Test
  void dataWrittenToResponseStream_isSentToClient() throws IOException {
    try (PrintStream ps = context.getResponseStream()) {
      ps.println("This is a line");
    }

    assertThat(response.getHtml().trim(), equalTo("This is a line"));
  }

  @Test
  void whenErrorCodeSent_statusAndContentAreSet() throws IOException {
    context.sendError(413, "That's OK");

    assertThat(response.getStatus(), equalTo(413));
    assertThat(response.getHtml().trim(), equalTo("That's OK"));
  }

  @Test
  void whenNullMessageSent_useSingleArgVersionOfServletResponse() throws IOException {
    context.sendError(413, null);

    assertThat(response.getStatus(), equalTo(413));
    assertThat(response.getHtml().trim(), equalTo(SINGLE_ARG_METHOD_CALLED));
  }

  @Test
  void whenRedirectSent_statusAndHeaderAreSet() throws IOException {
    context.sendRedirect("new/location");

    assertThat(response.getStatus(), equalTo(HTTP_MOVED_TEMP));
    assertThat(response.getRedirectLocation(), equalTo("new/location"));
  }

  @Test
  void whenResponseHeaderSet_isSetOnResponse() {
    context.setResponseHeader("Header", "value");

    assertThat(response.getHeaders("Header").stream().findFirst().orElse(null), equalTo("value"));
  }

  @Test
  void whenStatusSet_responseIsUpdated() {
    context.setStatus(371);

    assertThat(response.getStatus(), equalTo(371));
  }
}