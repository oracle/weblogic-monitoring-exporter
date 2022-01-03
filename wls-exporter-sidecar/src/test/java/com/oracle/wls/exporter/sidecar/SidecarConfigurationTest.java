// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.ArrayList;
import java.util.List;

import com.meterware.simplestub.Memento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.DEFAULT_LISTEN_PORT;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.DEFAULT_POD_NAME;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.DEFAULT_WLS_PORT;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.LISTEN_PORT_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.POD_NAME_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.WLS_HOST_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.WLS_PORT_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.WLS_SECURE_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SidecarConfigurationTest {

  private final List<Memento> mementos = new ArrayList<>();

  @BeforeEach
  void setUp() {
    SidecarConfigurationTestSupport.preserveConfigurationProperties(mementos);
  }

  @AfterEach
  void tearDown() {
    mementos.forEach(Memento::revert);
  }

  @Test
  void withoutSystemProperties_useDefaults() {
    final SidecarConfiguration configuration = new SidecarConfiguration();

    assertThat(configuration.getListenPort(), equalTo(DEFAULT_LISTEN_PORT));
    assertThat(configuration.getWebLogicPort(), equalTo(DEFAULT_WLS_PORT));
    assertThat(configuration.getWebLogicHost(), equalTo(SidecarConfiguration.getDefaultWlsHostName()));
    assertThat(configuration.useWebLogicSsl(), is(false));
    assertThat(configuration.getPodName(), equalTo(DEFAULT_POD_NAME));
  }

  @Test
  void whenListenPortPropertySpecified_useIt() {
    final int port = 8000;
    System.setProperty(LISTEN_PORT_PROPERTY, Integer.toString(port));

    final SidecarConfiguration configuration = new SidecarConfiguration();

    assertThat(configuration.getListenPort(), equalTo(port));
  }

  @Test
  void whenWebLogicPortPropertySpecified_useIt() {
    final int port = 8000;
    System.setProperty(WLS_PORT_PROPERTY, Integer.toString(port));

    final SidecarConfiguration configuration = new SidecarConfiguration();

    assertThat(configuration.getWebLogicPort(), equalTo(port));
  }

  @Test
  void whenWebLogicHostPropertySpecified_useIt() {
    final String hostName = "webLogicHost";
    System.setProperty(WLS_HOST_PROPERTY, hostName);

    final SidecarConfiguration configuration = new SidecarConfiguration();

    assertThat(configuration.getWebLogicHost(), equalTo(hostName));
  }

  @Test
  void whenWebLogicSecurePropertySpecified_useIt() {
    System.setProperty(WLS_SECURE_PROPERTY, "true");

    final SidecarConfiguration configuration = new SidecarConfiguration();

    assertThat(configuration.useWebLogicSsl(), is(true));
  }

  @Test
  void whenPodNamePropertySpecified_useIt() {
    final String podName = "server1";
    System.setProperty(POD_NAME_PROPERTY, podName);

    final SidecarConfiguration configuration = new SidecarConfiguration();

    assertThat(configuration.getPodName(), equalTo(podName));
  }
}
