// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;

import com.oracle.wls.exporter.domain.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static com.oracle.wls.exporter.InvocationContextStub.HOST_NAME;
import static com.oracle.wls.exporter.InvocationContextStub.PORT;
import static com.oracle.wls.exporter.InvocationContextStub.REST_PORT;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationFormCallTest {

  private static final String CONFIGURATION =
        "hostName: " + HOST_NAME + "\n" +
              "port: " + PORT + "\n" +
              "queries:\n" + "" +
              "- groups:\n" +
              "    prefix: new_\n" +
              "    key: name\n" +
              "    values: [sample1, sample2]\n";

  private static final String CONFIGURATION_WITH_REST_PORT =
        "hostName: " + HOST_NAME + "\n" +
              "port: " + PORT + "\n" +
              "restPort: " + REST_PORT + "\n" +
              "queries:\n" + "" +
              "- groups:\n" +
              "    prefix: new_\n" +
              "    key: name\n" +
              "    values: [sample1, sample2]\n";

  private static final String ADDED_CONFIGURATION =
        "hostName: localhost\n" +
              "port: 7001\n" +
              "queries:\n" + "" +
              "- people:\n" +
              "    key: name\n" +
              "    values: [age, sex]\n";

  private static final String COMBINED_CONFIGURATION =
        "hostName: " + HOST_NAME + "\n" +
              "port: " + PORT + "\n" +
              "queries:\n" + "" +
              "- groups:\n" +
              "    prefix: new_\n" +
              "    key: name\n" +
              "    values: [sample1, sample2]\n" +
              "- people:\n" +
              "    key: name\n" +
              "    values: [age, sex]\n";
  protected static final String BAD_BOOLEAN_STRING = "blabla";

  private final WebClientFactoryStub factory = new WebClientFactoryStub();
  private final InvocationContextStub context = InvocationContextStub.create();

  @BeforeEach
  public void setUp() {
    LiveConfiguration.setServer(HOST_NAME, PORT);
    LiveConfiguration.loadFromString(CONFIGURATION);
  }

  @Test
  void whenNoConfigurationSpecified_reportFailure() {
    assertThrows(RuntimeException.class, () -> handleConfigurationFormCall(context));
  }

  private void handleConfigurationFormCall(InvocationContextStub context) throws IOException {
    final ConfigurationFormCall call = new ConfigurationFormCall(factory, context);

    call.doWithAuthentication();
  }

  @Test
  void whenRequestUsesHttp_authenticateWithHttp() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("replace", CONFIGURATION));

    assertThat(factory.getClientUrl(), startsWith("http:"));
  }

  @Test
  void whenRequestUsesHttps_authenticateWithHttps() throws Exception {
    handleConfigurationFormCall(context.withHttps().withConfigurationForm("replace", CONFIGURATION));

    assertThat(factory.getClientUrl(), startsWith("https:"));
  }

  @Test
  void afterUploadWithReplace_useNewConfiguration() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("replace", CONFIGURATION));

    assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
  }

  @Test
  void afterUpload_redirectToMainPage() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("replace", CONFIGURATION));

    assertThat(context.getRedirectLocation(), equalTo(WebAppConstants.MAIN_PAGE));
  }

  @Test
  void whenRestPortInaccessible_switchToSpecifiedPort() throws Exception {
    LiveConfiguration.loadFromString(CONFIGURATION_WITH_REST_PORT);
    factory.throwConnectionFailure("localhost", REST_PORT);

    handleConfigurationFormCall(context.withConfigurationForm("replace", CONFIGURATION));

    assertThat(factory.getClientUrl(), containsString(Integer.toString(PORT)));
  }

  @Test
  void afterUploadWithAppend_useCombinedConfiguration() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("append", ADDED_CONFIGURATION));

    assertThat(LiveConfiguration.asString(), equalTo(COMBINED_CONFIGURATION));
  }

  @Test
  void whenSelectedFileIsNotYaml_reportError() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("replace", NON_YAML));

    assertThat(context.getResponse(), containsString(ConfigurationException.NOT_YAML_FORMAT));
  }

  private static final String NON_YAML =
        "this is not yaml\n";

  @Test
  void whenSelectedFileHasPartialYaml_reportError() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("replace", PARTIAL_YAML));

    assertThat(context.getResponse(), containsString(ConfigurationException.BAD_YAML_FORMAT));
  }

  private static final String PARTIAL_YAML =
        "queries:\nkey name\n";

  @Test
  void whenSelectedFileHasBadBooleanValue_reportError() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("append", ADDED_CONFIGURATION_WITH_BAD_BOOLEAN));

    assertThat(context.getResponse(), containsString(BAD_BOOLEAN_STRING));
  }

  private static final String ADDED_CONFIGURATION_WITH_BAD_BOOLEAN =
              "metricsNameSnakeCase: " + BAD_BOOLEAN_STRING + "\n" +
              "queries:\n" + "" +
              "- people:\n" +
              "    key: name\n" +
              "    values: [age, sex]\n";

  @Test
  void afterSelectedFileHasBadBooleanValue_configurationIsUnchanged() throws Exception {
    handleConfigurationFormCall(context.withConfigurationForm("append", ADDED_CONFIGURATION_WITH_BAD_BOOLEAN));

    assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
  }

  @Test
  void whenServerSends403StatusOnGet_returnToClient() throws Exception {
    factory.reportNotAuthorized();

    handleConfigurationFormCall(context.withConfigurationForm("replace", CONFIGURATION));

    assertThat(context.getResponseStatus(), equalTo(HTTP_FORBIDDEN));
  }

  @Test
  void whenServerSends401StatusOnGet_returnToClient() throws Exception {
    factory.reportAuthenticationRequired("Test-Realm");

    handleConfigurationFormCall(context.withConfigurationForm("replace", CONFIGURATION));

    assertThat(context.getResponseStatus(), equalTo(HTTP_UNAUTHORIZED));
    assertThat(context.getResponseHeader("WWW-Authenticate"), equalTo("Basic realm=\"Test-Realm\""));
  }
}