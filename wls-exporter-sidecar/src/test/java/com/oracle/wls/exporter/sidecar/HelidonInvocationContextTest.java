// Copyright (c) 2021, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.SystemPropertySupport;
import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.WebAppConstants;
import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Header;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.HttpMediaType;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.oracle.wls.exporter.sidecar.SidecarConfiguration.POD_NAME_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class HelidonInvocationContextTest {

  private static final String REQUEST_URL_PATTERN = "%s://%s:%d/address";
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
    request.setContentType(MediaTypes.APPLICATION_XML);

    assertThat(invocationContext.getContentType(), equalTo(MediaTypes.APPLICATION_XML.text()));
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
    public ServerRequestHeaders headers() {
      return headers;
    }
  }

  abstract static class RequestHeadersStub implements ServerRequestHeaders {

    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private HttpMediaType contentType;

    void addHeader(String name, String value) {
      headers.computeIfAbsent(name, it -> new ArrayList<>())
                      .add(value);
    }

    void setContentType(MediaType contentType) {
      this.contentType = HttpMediaType.create(contentType);
    }

    @Override
    public List<String> all(HeaderName name, Supplier<List<String>> supplier) {
      return headers.computeIfAbsent(name.defaultCase(), k -> new ArrayList<>());
    }

    @Override
    public boolean contains(HeaderName headerName) {
      return headers.containsKey(headerName.defaultCase());
    }

    @Override
    public boolean contains(Header header) {
      if (contains(header.headerName())) {
        List<String> myValues = headers.get(header.name());
        List<String> theirValues = header.allValues();
        return myValues.equals(theirValues);
      }
      return false;
    }

    @Override
    public Header get(HeaderName headerName) {
      if (contains(headerName)) {
        List<String> strings = headers.get(headerName.defaultCase());
        return HeaderValues.create(headerName, strings);
      }
      throw new NoSuchElementException("Header " + headerName + " not defined");
    }

    @Override
    public int size() {
      return headers.size();
    }

    @Override
    public List<HttpMediaType> acceptedTypes() {
      if (contains(HeaderNames.ACCEPT)) {
        return get(HeaderNames.ACCEPT)
                .stream()
                .map(HttpMediaType::create)
                .toList();
      }
      return List.of();
    }

    @Override
    public Iterator<Header> iterator() {
      return headers.entrySet()
              .stream()
              .map(it -> HeaderValues.create(it.getKey(), it.getValue()))
              .iterator();
    }

    @Override
    public Optional<HttpMediaType> contentType() {
      return Optional.ofNullable(contentType);
    }
  }

}