// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

public class SidecarConfiguration {

  static final String LISTEN_PORT_PROPERTY = "EXPORTER_PORT";
  static final String WLS_HOST_PROPERTY = "WLS_HOST";
  static final String WLS_PORT_PROPERTY = "WLS_PORT";
  static final String POD_NAME_PROPERTY = "POD_NAME";

  static final int DEFAULT_LISTEN_PORT = 8080;
  static final String DEFAULT_WLS_HOST = "localhost";
  static final int DEFAULT_WLS_PORT = 7001;
  static final String DEFAULT_POD_NAME = "<unknown>";

  private final int listenPort;
  private final String webLogicHost;
  private final int webLogicPort;
  private final String podName;

  public SidecarConfiguration() {
    listenPort = Integer.getInteger(LISTEN_PORT_PROPERTY, DEFAULT_LISTEN_PORT);
    webLogicHost = System.getProperty(WLS_HOST_PROPERTY, DEFAULT_WLS_HOST);
    webLogicPort = Integer.getInteger(WLS_PORT_PROPERTY, DEFAULT_WLS_PORT);
    podName = System.getProperty(POD_NAME_PROPERTY, DEFAULT_POD_NAME);
  }

  public int getListenPort() {
    return listenPort;
  }

  public int getWebLogicPort() {
    return webLogicPort;
  }

  public String getWebLogicHost() {
    return webLogicHost;
  }

  public String getPodName() {
    return podName;
  }
}
