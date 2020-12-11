// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.


package com.oracle.wls.exporter;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import static com.oracle.wls.exporter.HttpServletRequestStub.LOCAL_PORT;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class UrlBuilderTest {
  private final static String URL_PATTERN = "%s://%s:%d/path";
  private final static int REST_PORT = 7010;
  private final HttpServletRequest request = HttpServletRequestStub.createGetRequest();
  private final RestPortConnectionException connectionException = new RestPortConnectionException("http://localhost:7010");

  @Test
  public void whenNoRestPortDefined_generateUrl() {
    UrlBuilder builder = new UrlBuilder(request, null);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", "localhost", LOCAL_PORT)));
  }

  @Test
  public void whenRestPortDefined_generateUrlWithRestPort() {
    UrlBuilder builder = new UrlBuilder(request, REST_PORT);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", "localhost", REST_PORT)));
  }

  @Test(expected = RestPortConnectionException.class)
  public void whenNoRestPort_retryFails() {
    UrlBuilder builder = new UrlBuilder(request, null);
    builder.createUrl(URL_PATTERN);
    
    builder.reportFailure(connectionException);
  }

  @Test
  public void afterRestPortFails_retryWithLocalPort() {
    UrlBuilder builder = new UrlBuilder(request, REST_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", "localhost", LOCAL_PORT)));
  }

  @Test(expected = RestPortConnectionException.class)
  public void afterRestPortFails_secondRetryFails() {
    UrlBuilder builder = new UrlBuilder(request, REST_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);
    builder.reportFailure(connectionException);

    builder.createUrl(URL_PATTERN);
  }

  @Test
  public void afterLocalPortSucceeds_newBuilderPrefersLocalPort() {
    UrlBuilder builder = new UrlBuilder(request, REST_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);
    builder.createUrl(URL_PATTERN);
    builder.reportSuccess();

    UrlBuilder builder2 = new UrlBuilder(request, REST_PORT);
    assertThat(builder2.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", "localhost", LOCAL_PORT)));
  }
}
