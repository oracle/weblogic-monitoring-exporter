// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.


package com.oracle.wls.exporter;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class UrlBuilderTest {

  private static final String URL_PATTERN = "%s://%s:%d/path";
  private static final String HOST = "wlsHost";
  private static final boolean CLEARTEXT = false;
  private static final int REST_PORT = 7010;
  private static final int LOCAL_PORT = 7356;
  private final RestPortConnectionException connectionException = new RestPortConnectionException("http://localhost:7010");

  @Test
  public void whenNoRestPortDefined_generateUrl() {
    UrlBuilder builder = createUrlBuilder().withPort(LOCAL_PORT);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOST, LOCAL_PORT)));
  }

  private UrlBuilder createUrlBuilder() {
    return UrlBuilder.create(HOST, CLEARTEXT);
  }

  @Test
  public void whenRestPortDefined_generateUrlWithRestPort() {
    UrlBuilder builder = createUrlBuilder().withPort(REST_PORT).withPort(LOCAL_PORT);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOST, REST_PORT)));
  }

  @Test(expected = RestPortConnectionException.class)
  public void whenNoRestPortAndConnectionFails_reportFailure() {
    UrlBuilder builder = createUrlBuilder().withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    
    builder.reportFailure(connectionException);
  }

  @Test
  public void afterRestPortFails_retryWithLocalPort() {
    UrlBuilder builder = createUrlBuilder().withPort(REST_PORT).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOST, LOCAL_PORT)));
  }

  @Test(expected = RestPortConnectionException.class)
  public void afterRestPortFailsAndSecondRetryFails_reportFailure() {
    UrlBuilder builder = createUrlBuilder().withPort(REST_PORT).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);
    builder.reportFailure(connectionException);

    builder.createUrl(URL_PATTERN);
  }

  @Test
  public void afterLocalPortSucceeds_newBuilderPrefersLocalPort() {
    UrlBuilder builder = createUrlBuilder().withPort(REST_PORT).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);
    builder.createUrl(URL_PATTERN);
    builder.reportSuccess();

    UrlBuilder builder2 = createUrlBuilder().withPort(REST_PORT).withPort(LOCAL_PORT);
    assertThat(builder2.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOST, LOCAL_PORT)));
  }
}
