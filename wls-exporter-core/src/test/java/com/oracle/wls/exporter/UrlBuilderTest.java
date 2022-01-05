// Copyright (c) 2020, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.


package com.oracle.wls.exporter;

import java.util.Set;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UrlBuilderTest {

  private static final String URL_PATTERN = "%s://%s:%d/path";
  private static final String REST_HOSTNAME = "restHost";
  private static final String HOSTNAME = "wlsHost";
  private static final boolean CLEARTEXT = false;
  private static final int REST_PORT = 7010;
  private static final int LOCAL_PORT = 7356;
  private final RestPortConnectionException connectionException = new RestPortConnectionException("http://localhost:7010");
  private Memento successMemento;

  @BeforeEach
  void setUp() throws NoSuchFieldException {
    successMemento = StaticStubSupport.preserve(UrlBuilder.class, "successes");
  }

  @AfterEach
  void tearDown() {
    successMemento.<Set<Object>>getOriginalValue().clear();
  }

  @Test
  public void whenNoRestPortOrHostNameDefined_generateUrl() {
    UrlBuilder builder = createUrlBuilder().withHostName(HOSTNAME).withPort(LOCAL_PORT);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOSTNAME, LOCAL_PORT)));
  }

  private UrlBuilder createUrlBuilder() {
    return UrlBuilder.create(CLEARTEXT);
  }

  @Test
  public void whenRestPortDefined_generateUrlWithRestPort() {
    UrlBuilder builder = createUrlBuilder().withHostName(HOSTNAME).withPort(REST_PORT).withPort(LOCAL_PORT);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOSTNAME, REST_PORT)));
  }

  @Test
  public void whenNoRestPortAndConnectionFails_reportFailure() {
    UrlBuilder builder = createUrlBuilder().withHostName(HOSTNAME).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    
    assertThrows(RestPortConnectionException.class, () -> builder.reportFailure(connectionException));
  }

  @Test
  public void afterRestPortFails_retryWithLocalPort() {
    UrlBuilder builder = createUrlBuilder().withHostName(HOSTNAME).withPort(REST_PORT).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOSTNAME, LOCAL_PORT)));
  }

  @Test
  public void afterRestPortFailsAndSecondRetryFails_reportFailure() {
    UrlBuilder builder = createUrlBuilder().withHostName(HOSTNAME).withPort(REST_PORT).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);

    assertThrows(RestPortConnectionException.class, () -> builder.reportFailure(connectionException));
  }

  @Test
  public void afterLocalPortSucceeds_newBuilderPrefersLocalPort() {
    UrlBuilder builder = createUrlBuilder().withHostName(HOSTNAME).withPort(REST_PORT).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);
    builder.createUrl(URL_PATTERN);
    builder.reportSuccess();

    UrlBuilder builder2 = createUrlBuilder().withHostName(HOSTNAME).withPort(REST_PORT).withPort(LOCAL_PORT);
    assertThat(builder2.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOSTNAME, LOCAL_PORT)));
  }

  @Test
  public void afterRestHostNameFails_retryWithBackupHost() {
    UrlBuilder builder = createUrlBuilder().withHostName(REST_HOSTNAME).withHostName(HOSTNAME).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);

    assertThat(builder.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOSTNAME, LOCAL_PORT)));
  }

  @Test
  public void afterRestHostNameFailsAndSecondRetryFails_reportFailure() {
    UrlBuilder builder = createUrlBuilder().withHostName(REST_HOSTNAME).withHostName(HOSTNAME).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);

    assertThrows(RestPortConnectionException.class, () -> builder.reportFailure(connectionException));
  }

  @Test
  public void afterLocalHostSucceeds_newBuilderPrefersLocalHost() {
    UrlBuilder builder = createUrlBuilder().withHostName(REST_HOSTNAME).withHostName(HOSTNAME).withPort(LOCAL_PORT);
    builder.createUrl(URL_PATTERN);
    builder.reportFailure(connectionException);
    builder.createUrl(URL_PATTERN);
    builder.reportSuccess();

    UrlBuilder builder2 = createUrlBuilder().withHostName(REST_HOSTNAME).withHostName(HOSTNAME).withPort(LOCAL_PORT);
    assertThat(builder2.createUrl(URL_PATTERN), equalTo(String.format(URL_PATTERN, "http", HOSTNAME, LOCAL_PORT)));
  }
}
