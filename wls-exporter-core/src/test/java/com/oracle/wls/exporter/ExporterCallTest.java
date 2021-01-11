// Copyright (c) 2021, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
package com.oracle.wls.exporter;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import static com.oracle.wls.exporter.InvocationContextStub.HOST;
import static com.oracle.wls.exporter.InvocationContextStub.PORT;
import static com.oracle.wls.exporter.InvocationContextStub.REST_PORT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class ExporterCallTest {
  private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
  private static final String SECURE_URL_PATTERN = "https://%s:%d/management/weblogic/latest/serverRuntime/search";
  private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
  private static final String TWO_VALUE_CONFIG = "queries:" +
          "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]";
  private static final String REST_PORT_CONFIG = "restPort: " + REST_PORT +
                                                 "\nqueries:\n- groups:\n    key: name\n    values: testSample1";
  private static final String CONFIG_WITH_CATEGORY_VALUE = "queries:" +
          "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1, testSample2, bogus]";
  private static final String MULTI_QUERY_CONFIG = "queries:" +
          "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]" +
          "\n- colors:                         \n    key: hue \n    values: wavelength";

  private final WebClientFactoryStub factory = new WebClientFactoryStub();
  private final InvocationContextStub context = InvocationContextStub.create();

  @Before
  public void setUp() {
    LiveConfiguration.setServer(HOST, PORT);
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

      assertThat(factory.getClientUrl(), equalTo(String.format(URL_PATTERN, HOST, PORT)));
  }

  @Test
  public void onSecurePlaintextGet_defineConnectionUrlFromContext() throws Exception {
    LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);

    handleMetricsCall(context.withHttps());

      assertThat(factory.getClientUrl(), equalTo(String.format(SECURE_URL_PATTERN, HOST, PORT)));
  }

  //@Test
  public void whenRestPortDefined_connectionUrlUsesRestPort() throws IOException {
    LiveConfiguration.loadFromString(REST_PORT_CONFIG);

    handleMetricsCall(context);

      assertThat(factory.getClientUrl(),  equalTo(String.format(URL_PATTERN, HOST, REST_PORT)));
  }
}