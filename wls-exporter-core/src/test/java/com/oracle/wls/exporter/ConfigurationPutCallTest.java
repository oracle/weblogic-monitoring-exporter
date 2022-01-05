// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.InvocationContextStub.HOST_NAME;
import static com.oracle.wls.exporter.InvocationContextStub.PORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ConfigurationPutCallTest {

  private static final String BAD_BOOLEAN_STRING = "blabla";
  private static final String YAML_CONFIGURATION =
        "hostName: " + HOST_NAME + "\n" +
        "port: " + PORT + "\n" +
        "queries:\n" + "" +
        "- groups:\n" +
        "    prefix: new_\n" +
        "    key: name\n" +
        "    values: [sample1, sample2]\n";
  private static final String JSON_CONFIGURATION = replaceQuotes(
        "{'queries': [{'groups':"
              + "{'prefix':'new_', 'key':'name', 'values': ['sample1','sample2']}"
              + "}]}");

  private static final String NO_CONFIGURATION = "";
  private final WebClientFactoryStub factory = new WebClientFactoryStub();
  private final InvocationContextStub context = InvocationContextStub.create();

  @SuppressWarnings("SameParameterValue")
  private static String replaceQuotes(String in) {
    return in.replace("'", "\"");
  }

  @BeforeEach
  void setUp() {
    LiveConfiguration.setServer(HOST_NAME, PORT);
    LiveConfiguration.loadFromString(NO_CONFIGURATION);
  }

  @Test
  void whenBadRequestContentType_reportFailure() throws IOException {
    handleConfigurationPutCall(context.withConfiguration("text/xml", YAML_CONFIGURATION));

    assertThat(context.getResponseStatus(), equalTo(HttpURLConnection.HTTP_BAD_REQUEST));
  }

  private void handleConfigurationPutCall(InvocationContextStub context) throws IOException {
    final ConfigurationPutCall call = new ConfigurationPutCall(factory, context);

    call.doWithAuthentication();
  }

  @Test
  public void whenSpecifiedConfigurationHasBadBooleanValue_reportError() throws Exception {
    handleConfigurationPutCall(context.withConfiguration("application/yaml", CONFIGURATION_WITH_BAD_BOOLEAN));

    assertThat(context.getResponseStatus(), equalTo(HttpURLConnection.HTTP_BAD_REQUEST));
  }

  @Test
  public void updateSpecifiedConfiguration() throws Exception {
    handleConfigurationPutCall(context.withConfiguration("application/yaml", YAML_CONFIGURATION));

    assertThat(LiveConfiguration.asString(), equalTo(YAML_CONFIGURATION));
  }

  @Test
  public void updateConfigurationWithJson() throws Exception {
    handleConfigurationPutCall(context.withConfiguration("application/json", JSON_CONFIGURATION));

    assertThat(LiveConfiguration.asString(), equalTo(YAML_CONFIGURATION));
  }

  private static final String CONFIGURATION_WITH_BAD_BOOLEAN =
              "metricsNameSnakeCase: " + BAD_BOOLEAN_STRING + "\n" +
              "queries:\n" + "" +
              "- people:\n" +
              "    key: name\n" +
              "    values: [age, sex]\n";
}