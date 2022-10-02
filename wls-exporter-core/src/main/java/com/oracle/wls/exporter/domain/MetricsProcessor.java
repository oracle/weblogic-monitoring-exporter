// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.Map;

interface MetricsProcessor {

  /**
   * Uses the specified metrics to update configuration information.
   * @param metrics a set of data scraped from a WebLogic Server response
   */
  void updateConfiguration(Map<String, Object> metrics);
}
