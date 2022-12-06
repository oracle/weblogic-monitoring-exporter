// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

class AuthenticatedCallTest {

  private final WebClientFactoryStub webClientFactory = new WebClientFactoryStub();
  private final InvocationContextStub invocationContext = InvocationContextStub.create();
  private final CallStub callStub = new CallStub(webClientFactory, invocationContext);

  @Test
  void whenInvocationContextContainsNoCookieHeaders_callToServerContinues() throws IOException {

    callStub.doWithAuthentication();

    assertThat(webClientFactory.getSentHeaders("Cookie"), empty());
  }

  @Test
  void whenInvocationContextContainsCookieHeaders_forwardThemToTheServer() throws IOException {
    invocationContext.addCookie("cookie1", "value1");
    invocationContext.addCookie("cookie2", "value2");

    callStub.doWithAuthentication();

    final List<String> cookieHeaders
          = webClientFactory.getSentHeaders("Cookie").stream()
          .map(this::trimParameters)
          .collect(Collectors.toList());
    assertThat(cookieHeaders, containsInAnyOrder("cookie1=value1", "cookie2=value2"));
  }

  private String trimParameters(String cookieHeader) {
    if (!cookieHeader.contains(";"))
      return cookieHeader;
    else
      return cookieHeader.substring(0, cookieHeader.indexOf(';'));
  }

  @Test
  void whenResponseFromRestApiContainsSetCookieHeaders_forwardThemToTheClient() throws IOException {
    webClientFactory.forJson("{}")
          .withResponseHeader("Set-Cookie", "cookie1=value1; http-only")
          .withResponseHeader("Set-Cookie", "cookie2=value2; secure")
          .addResponse();

    callStub.doWithAuthentication();

    final List<String> setCookieHeaders
          = invocationContext.getResponseHeaders("Set-Cookie").stream()
              .map(this::trimParameters)
              .collect(Collectors.toList());
    assertThat(setCookieHeaders, containsInAnyOrder("cookie1=value1", "cookie2=value2"));
  }

  static class CallStub extends AuthenticatedCall {
    CallStub(WebClientFactory webClientFactory, InvocationContext context) {
      super(webClientFactory, context);
    }

    @Override
    protected void invoke(WebClient webClient, InvocationContext context) throws IOException {
      webClient.withUrl(getAuthenticationUrl()).doPostRequest("abcd");
    }
  }


}