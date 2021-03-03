// Copyright (c) 2021, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.IOException;

import com.oracle.wls.exporter.CallFactory;
import com.oracle.wls.exporter.ConfigurationFormCall;
import com.oracle.wls.exporter.ExporterCall;
import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.WebClientFactory;

public class CallFactoryServletImpl implements CallFactory {

  private final WebClientFactory factory;

  public CallFactoryServletImpl(WebClientFactory factory) {
    this.factory = factory;
  }

  @Override
  public void invokeConfigurationFormCall(InvocationContext invocationContext) throws IOException {
    new ConfigurationFormCall(factory, invocationContext).doWithAuthentication();
  }

  @Override
  public void invokeMetricsCall(InvocationContext invocationContext) throws IOException {
    new ExporterCall(factory, invocationContext).doWithAuthentication();
  }
}
