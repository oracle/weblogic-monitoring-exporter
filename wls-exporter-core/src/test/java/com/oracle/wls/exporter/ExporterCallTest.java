// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.oracle.wls.exporter.InvocationContextStub.HOST_NAME;
import static com.oracle.wls.exporter.InvocationContextStub.PORT;
import static com.oracle.wls.exporter.WebAppConstants.COOKIE_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.SET_COOKIE_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class ExporterCallTest {
  private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
  private static final String SECURE_URL_PATTERN = "https://%s:%d/management/weblogic/latest/serverRuntime/search";
  private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
  private static final String CONFIG_WITH_FILTER = "queries:" +
        "\n- groups:\n    key: name\n    includedKeyValues: abc.*\n    values: testSample1";
  private static final String DUAL_QUERY_CONFIG = ONE_VALUE_CONFIG +
        "\n- clubs:\n    key: name\n    values: testSample2";

  private static final String KEY_RESPONSE_JSON = "{\"groups\": {\"items\": [\n" +
              "     {\"name\": \"alpha\"},\n" +
              "     {\"name\": \"beta\" },\n" +
              "     {\"name\": \"gamma\"}\n" +
              "]}}";

  private static final String QUERY_RESPONSE1_JSON = "{\"groups\": {\"items\": [\n" +
              "     {\"name\": \"alpha\", \"testSample1\": \"first\"},\n" +
              "     {\"name\": \"beta\", \"testSample1\": \"second\"},\n" +
              "     {\"name\": \"gamma\", \"testSample1\": \"third\"}\n" +
              "]}}";

  private static final String QUERY_RESPONSE2_JSON = "{\"clubs\": {\"items\": [\n" +
              "     {\"name\": \"aleph\", \"testSample2\": \"first\"},\n" +
              "     {\"name\": \"bet\", \"testSample2\": \"second\"},\n" +
              "     {\"name\": \"gimel\", \"testSample2\": \"third\"}\n" +
              "]}}";

  private final WebClientFactoryStub factory = new WebClientFactoryStub();
  private final InvocationContextStub context = InvocationContextStub.create();

  @BeforeEach
  public void setUp() {
    LiveConfiguration.setServer(HOST_NAME, PORT);
    AuthenticatedCall.clearCookies();
  }

  @Test
  void whenConfigFileNameNotFound_getReportsTheIssue() throws Exception {
    LiveConfiguration.loadFromString("");

    handleMetricsCall(context);

    assertThat(context.getResponse(), containsString("# No configuration"));
  }

  private void handleMetricsCall(InvocationContextStub context) throws IOException {
    final AuthenticatedCall call = new ExporterCall(factory, context);

    call.doWithAuthentication();
  }

  @Test
  void onPlaintextGet_defineConnectionUrlFromContext() throws Exception {
    LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);

    handleMetricsCall(context);

    assertThat(factory.getClientUrl(), equalTo(String.format(URL_PATTERN, HOST_NAME, PORT)));
  }

  @Test
  void onSecurePlaintextGet_defineConnectionUrlFromContext() throws Exception {
    LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);

    handleMetricsCall(context.withHttps());

    assertThat(factory.getClientUrl(), equalTo(String.format(SECURE_URL_PATTERN, HOST_NAME, PORT)));
  }

  @Test
  void whenQuerySentWithoutFilter_onlyOneRequestIsMade() throws IOException {
    LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);

    handleMetricsCall(context.withHttps());

    assertThat(factory.getNumQueriesSent(), equalTo(1));
  }

  @Test
  void whenQuerySentWithFilter_firstQueryRequestsPossibleKeys() throws IOException {
    factory.addJsonResponse(KEY_RESPONSE_JSON);
    LiveConfiguration.loadFromString(CONFIG_WITH_FILTER);

    handleMetricsCall(context.withHttps());

    assertThat(factory.getSentQuery(), hasJsonPath("$.children.groups.fields", contains("name")));
  }

  @Test
  void whenQuerySentWithFilter_twoRequestsAreMade() throws IOException {
    factory.addJsonResponse(KEY_RESPONSE_JSON);
    LiveConfiguration.loadFromString(CONFIG_WITH_FILTER);

    handleMetricsCall(context.withHttps());

    assertThat(factory.getNumQueriesSent(), equalTo(2));
  }

  @Test
  void whenHaveMultipleQueries_sendServerDefinedCookieOnSecondQuery() throws IOException {
    factory.forJson(QUERY_RESPONSE1_JSON).withResponseHeader(SET_COOKIE_HEADER, "cookieName=newValue").addResponse();
    factory.addJsonResponse(QUERY_RESPONSE2_JSON);
    LiveConfiguration.loadFromString(DUAL_QUERY_CONFIG);

    handleMetricsCall(context);

    assertThat(factory.getSentHeaders(COOKIE_HEADER), Matchers.hasItem("cookieName=newValue"));
  }
}