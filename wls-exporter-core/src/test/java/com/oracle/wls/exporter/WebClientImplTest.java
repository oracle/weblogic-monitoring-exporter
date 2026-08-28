// Copyright (c) 2019, 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import org.apache.http.client.config.RequestConfig;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class WebClientImplTest extends WebClientTestBase {

  public WebClientImplTest() {
    super(WebClientImpl::new);
  }

  @Test
  void whenRequestTimeoutConfigured_applyItToAllApacheTimeouts() {
    WebClientImpl client = new WebClientImpl();
    client.setRequestTimeoutMillis(15000);

    RequestConfig config = client.createRequestConfig();

    assertThat(config.getConnectTimeout(), equalTo(15000));
    assertThat(config.getSocketTimeout(), equalTo(15000));
    assertThat(config.getConnectionRequestTimeout(), equalTo(15000));
  }
}
