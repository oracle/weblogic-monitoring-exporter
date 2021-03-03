// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.SystemPropertySupport;
import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.WebAppConstants;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.RequestHeaders;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.POD_NAME_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class HelidonInvocationContextTest {

  private final static String HOST = "localhost";
  private final static int WLS_PORT = 8765;
  private final static String REQUEST_URL_PATTERN = "%s://%s:%d/address";
  private static final String POD_NAME = "server2";

  private final ServerRequestStub request = createStrictStub(ServerRequestStub.class);
  private final List<Memento> mementos = new ArrayList<>();

  private InvocationContext invocationContext;
  private final SidecarConfiguration configuration = new SidecarConfiguration();

  @BeforeEach
  void setUp() {
    mementos.add(SystemPropertySupport.install(POD_NAME_PROPERTY, POD_NAME));

    invocationContext = new HelidonInvocationContext(request, createStrictStub(ServerResponse.class));
  }

  @AfterEach
  void tearDown() {
    mementos.forEach(Memento::revert);
  }

  @Test
  void requestUrl_usesSidecarConfiguration() {
    assertThat(invocationContext.createUrlBuilder().createUrl(REQUEST_URL_PATTERN),
               equalTo(String.format(REQUEST_URL_PATTERN,
                   "http", configuration.getWebLogicHost(), configuration.getWebLogicPort())));
  }

  @Test
  void applicationContextIsEmpty() {
    assertThat(invocationContext.getApplicationContext(), equalTo("/"));
  }

  @Test
  void whenNoAuthenticationHeaderSpecified_returnNull() {
    assertThat(invocationContext.getAuthenticationHeader(), nullValue());
  }

  @Test
  void whenAuthenticationHeaderSpecified_returnIt() {
    request.addHeader(WebAppConstants.AUTHENTICATION_HEADER, "asdef");

    assertThat(invocationContext.getAuthenticationHeader(), equalTo("asdef"));
  }

  @Test
  void returnSpecifiedContentType() {
    request.setContentType(MediaType.APPLICATION_XML);

    assertThat(invocationContext.getContentType(), equalTo(MediaType.APPLICATION_XML.toString()));
  }

  @Test
  void getInstanceName_returnsPodName() {
    assertThat(invocationContext.getInstanceName(), equalTo(POD_NAME));
  }

  abstract static class ServerRequestStub implements ServerRequest {
    private final RequestHeadersStub headers = createStrictStub(RequestHeadersStub.class);

    @SuppressWarnings("SameParameterValue")
    void addHeader(String name, String value) {
      headers.addHeader(name, value);
    }

    @SuppressWarnings("SameParameterValue")
    void setContentType(MediaType contentType) {
        headers.setContentType(contentType);
    }

    @Override
    public RequestHeaders headers() {
      return headers;
    }
  }

  abstract static class RequestHeadersStub implements RequestHeaders {

    private final Map<String, String> headers = new HashMap<>();
    private MediaType contentType;

    void addHeader(String name, String value) {
      headers.put(name, value);
    }

    void setContentType(MediaType contentType) {
      this.contentType = contentType;
    }

    @Override
    public Optional<String> first(String name) {
      return Optional.ofNullable(headers.get(name));
    }

    @Override
    public Optional<MediaType> contentType() {
      return Optional.ofNullable(contentType);
    }
  }

}