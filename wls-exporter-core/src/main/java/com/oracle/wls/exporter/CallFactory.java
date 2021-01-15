// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;

public interface CallFactory {

  /**
   * Creates processing to handle a form submission to update the exporter configuration.
   * @param invocationContext context of the invocation, containing request and response objects.
   */
  void invokeConfigurationFormCall(InvocationContext invocationContext) throws IOException;

  /**
   * Creates processing to handle a request for metrics.
   * @param invocationContext context of the invocation, containing request and response objects.
   */
  void invokeMetricsCall(InvocationContext invocationContext) throws IOException;
}
