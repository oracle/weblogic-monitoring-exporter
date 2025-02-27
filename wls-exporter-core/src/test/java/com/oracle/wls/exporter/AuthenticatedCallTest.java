// Copyright (c) 2023, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.meterware.simplestub.Memento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.InvocationContextStub.CREDENTIALS;
import static com.oracle.wls.exporter.WebAppConstants.COOKIE_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.SET_COOKIE_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

class AuthenticatedCallTest {

  private final WebClientFactoryStub webClientFactory = new WebClientFactoryStub();
  private final InvocationContextStub invocationContext = InvocationContextStub.create();
  private final AuthenticatedCallStub callStub = new AuthenticatedCallStub(webClientFactory, invocationContext);
  private final List<Memento> mementos = new ArrayList<>();

  @BeforeEach
  void setUp() throws NoSuchFieldException {
    AuthenticatedCall.clearCookies();

    mementos.add(SystemClockTestSupport.installClock());
  }

  @AfterEach
  void tearDown() {
    mementos.forEach(Memento::revert);
  }

  @Test
  void whenInvocationContextContainsNoCookieHeaders_callToServerContinues() throws IOException {

    callStub.doWithAuthentication();

    assertThat(webClientFactory.getSentHeaders(COOKIE_HEADER), empty());
  }

  @Test
  void withNoAuthenticationWhenResponseFromRestApiContainsSetCookieHeaders_ignoreThem() throws IOException {
    invocationContext.setAuthenticationHeader(null);
    configureResponseWithSetCookieHeaders();

    callStub.doWithAuthentication();

    assertThat(callStub.getCookies(CREDENTIALS), empty());
  }

  private void configureResponseWithSetCookieHeaders() {
    webClientFactory.forJson("{}")
          .withResponseHeader(SET_COOKIE_HEADER, "cookie1=value1; http-only")
          .withResponseHeader(SET_COOKIE_HEADER, "cookie2=value2; secure")
          .addResponse();
  }

  @Test
  void whenResponseFromRestApiContainsSetCookieHeaders_cacheThem() throws IOException {
    configureResponseWithSetCookieHeaders();

    callStub.doWithAuthentication();

    assertThat(callStub.getCookies(CREDENTIALS), containsInAnyOrder("cookie1=value1", "cookie2=value2"));
  }

  @Test
  void whenMultipleResponsesContainsSetCookieHeadersWithTheSameName_keepOnlyTheLast() throws IOException {
    handleServerCookieWithValue("value1");
    handleServerCookieWithValue("value2");

    assertThat(callStub.getCookies(CREDENTIALS), containsInAnyOrder("cookie1=value2"));
  }

  private void handleServerCookieWithValue(String cookieValue) throws IOException {
    webClientFactory.forJson("{}")
          .withResponseHeader(SET_COOKIE_HEADER, "cookie1=" + cookieValue)
          .addResponse();
    callStub.doWithAuthentication();
  }

  @Test
  void whenResponseFromRestApiContainsSetCookieHeaders_ignoreForDifferentCredentials() throws IOException {
    configureResponseWithSetCookieHeaders();

    callStub.doWithAuthentication();

    assertThat(callStub.getCookies("other credentials"), empty());
  }

  @Test
  void whenInvocationContextContainsCookieHeaders_forwardThemToTheServer() throws IOException {
    callStub.handleNewCookie("cookie1=value1; http-only");
    callStub.handleNewCookie("cookie2=value2; secure");
    webClientFactory.forJson("{}").addResponse();
    
    callStub.doWithAuthentication();

    final List<String> cookieHeaders
          = webClientFactory.getSentHeaders(COOKIE_HEADER).stream()
          .map(this::trimParameters)
          .collect(Collectors.toList());
    assertThat(cookieHeaders, containsInAnyOrder("cookie1=value1", "cookie2=value2"));
  }

  @Test
  void wheCookiesExpired_removeThem() throws IOException {
    callStub.handleNewCookie("cookie1=value1; http-only");
    callStub.handleNewCookie("cookie2=value2; secure");
    webClientFactory.forJson("{}").addResponse();

    SystemClockTestSupport.increment(AuthenticatedCall.COOKIE_LIFETIME_SECONDS + 1);
    callStub.doWithAuthentication();

    final List<String> cookieHeaders
          = webClientFactory.getSentHeaders(COOKIE_HEADER).stream()
          .map(this::trimParameters)
          .collect(Collectors.toList());
    assertThat(cookieHeaders, empty());
  }

  private String trimParameters(String cookieHeader) {
    if (!cookieHeader.contains(";"))
      return cookieHeader;
    else
      return cookieHeader.substring(0, cookieHeader.indexOf(';'));
  }

  static class AuthenticatedCallStub extends AuthenticatedCall {
    AuthenticatedCallStub(WebClientFactory webClientFactory, InvocationContext context) {
      super(webClientFactory, context);
    }

    @Override
    protected void invoke(WebClient webClient, InvocationContext context) throws IOException {
      webClient.withUrl(getAuthenticationUrl()).doPostRequest("abcd");
    }
  }


}