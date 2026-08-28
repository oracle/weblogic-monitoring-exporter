// Copyright (c) 2020, 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class WebClient8ImplTest extends WebClientTestBase {
  public WebClient8ImplTest() {
    super(WebClient8Impl::new);
  }

  @Test
  void whenRequestTimeoutConfigured_applyItToJava8Connection() throws Exception {
    WebClient8Impl client = new WebClient8Impl();
    client.setRequestTimeoutMillis(15000);
    HttpURLConnection connection = new TestHttpURLConnection();

    client.configureTimeouts(connection);

    assertThat(connection.getConnectTimeout(), equalTo(15000));
    assertThat(connection.getReadTimeout(), equalTo(15000));
  }

  private static class TestHttpURLConnection extends HttpURLConnection {
    TestHttpURLConnection() throws IOException {
      super(new URL("http://localhost"));
    }

    @Override
    public void disconnect() {
      // no-op
    }

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public void connect() {
      // no-op
    }
  }
}
