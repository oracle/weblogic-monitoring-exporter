// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.Arrays;
import java.util.List;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.SystemPropertySupport;

import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.LISTEN_PORT_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.POD_NAME_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.WLS_HOST_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.WLS_PORT_PROPERTY;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.WLS_SECURE_PROPERTY;

public class SidecarConfigurationTestSupport {
  private static final String[] CONFIGURATION_PROPERTIES
        = {LISTEN_PORT_PROPERTY, POD_NAME_PROPERTY, WLS_HOST_PROPERTY, WLS_PORT_PROPERTY, WLS_SECURE_PROPERTY};

  static void preserveConfigurationProperties(List<Memento> mementos) {
    Arrays.stream(CONFIGURATION_PROPERTIES).forEach(property -> preserveAndClearProperty(mementos, property));
  }

  private static void preserveAndClearProperty(List<Memento> mementos, String property) {
    mementos.add(SystemPropertySupport.preserve(property));
    System.clearProperty(property);
  }
}
