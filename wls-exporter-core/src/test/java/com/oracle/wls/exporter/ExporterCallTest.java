// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
package com.oracle.wls.exporter;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.InvocationContextStub.HOST_NAME;
import static com.oracle.wls.exporter.InvocationContextStub.PORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class ExporterCallTest {
  private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
  private static final String SECURE_URL_PATTERN = "https://%s:%d/management/weblogic/latest/serverRuntime/search";
  private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";

  private final WebClientFactoryStub factory = new WebClientFactoryStub();
  private final InvocationContextStub context = InvocationContextStub.create();

  @BeforeEach
  public void setUp() {
    LiveConfiguration.setServer(HOST_NAME, PORT);
//    LiveConfiguration.loadFromString(CONFIGURATION);
  }

  @Test
  public void whenConfigFileNameNotFound_getReportsTheIssue() throws Exception {
    LiveConfiguration.loadFromString("");

    handleMetricsCall(context);

    assertThat(context.getResponse(), containsString("# No configuration"));
  }

  private void handleMetricsCall(InvocationContextStub context) throws IOException {
    final AuthenticatedCall call = new ExporterCall(factory, context);

    call.doWithAuthentication();
  }

  @Test
  public void onPlaintextGet_defineConnectionUrlFromContext() throws Exception {
    LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);

    handleMetricsCall(context);

      assertThat(factory.getClientUrl(), equalTo(String.format(URL_PATTERN, HOST_NAME, PORT)));
  }

  @Test
  public void onSecurePlaintextGet_defineConnectionUrlFromContext() throws Exception {
    LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);

    handleMetricsCall(context.withHttps());

      assertThat(factory.getClientUrl(), equalTo(String.format(SECURE_URL_PATTERN, HOST_NAME, PORT)));
  }
}