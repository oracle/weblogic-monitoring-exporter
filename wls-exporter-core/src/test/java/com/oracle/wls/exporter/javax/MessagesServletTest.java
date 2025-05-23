// Copyright (c) 2019, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.javax;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.stream.IntStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.oracle.wls.exporter.WebClientFactoryStub;
import com.oracle.wls.exporter.WlsRestExchanges;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.WlsRestExchanges.MAX_EXCHANGES;
import static com.oracle.wls.exporter.javax.HttpServletRequestStub.createGetRequest;
import static com.oracle.wls.exporter.javax.HttpServletResponseStub.createServletResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class MessagesServletTest {

  private static final int EXCESS_EXCHANGES = 3;
  private static final String URL = "http://localhost:7001";
  private final WebClientFactoryStub factory = new WebClientFactoryStub();
  private final MessagesServlet servlet = new MessagesServlet(factory);
  private final HttpServletRequestStub request = createGetRequest();
  private final HttpServletResponseStub response = createServletResponse();

  @BeforeEach
  void setUp() {
    WlsRestExchanges.clear();
  }

  @Test
  void diagnostics_isHttpServlet() {
    assertThat(servlet, instanceOf(HttpServlet.class));
  }

  @Test
  void servlet_isPublic() {
    assertThat(Modifier.isPublic(MessagesServlet.class.getModifiers()), is(true));
  }

  @Test
  void servlet_hasWebServletAnnotation() {
    assertThat(MessagesServlet.class.getAnnotation(WebServlet.class), notNullValue());
  }

  @Test
  void servletAnnotationIndicatesMetricsPage() {
    WebServlet annotation = MessagesServlet.class.getAnnotation(WebServlet.class);

    assertThat(annotation.value(), arrayContaining("/messages"));
  }

  @Test
  void afterExchangeAdded_retrieveExchange() {
    WlsRestExchanges.addExchange(URL, "request 1", "response1");

    assertThat(WlsRestExchanges.getExchanges(),
          hasItem(both(containsString("request 1")).and(containsString("response1"))));
  }

  @Test
  void afterMaximumExchangesAdded_retrieveAllExchanges() {
    IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addTestExchange);

    assertThat(WlsRestExchanges.getExchanges(), contains(getExchangeMatchers(1, MAX_EXCHANGES)));
  }

  private void addTestExchange(int i) {
    WlsRestExchanges.addExchange(URL, "request " + i, "response " + i);
  }

  @SuppressWarnings("unchecked")
  private Matcher<String>[] getExchangeMatchers(int first, int last) {
    return IntStream.rangeClosed(first, last).mapToObj(this::getExchangeMatcher).toArray(Matcher[]::new);
  }

  private Matcher<String> getExchangeMatcher(int i) {
    return containsString("request " + i);
  }

  @Test
  void afterMoreThanMaximumExchangesAdded_retrieveLastMExchanges() {
    IntStream.rangeClosed(1, MAX_EXCHANGES + EXCESS_EXCHANGES).forEach(this::addTestExchange);

    assertThat(WlsRestExchanges.getExchanges(),
          contains(getExchangeMatchers(EXCESS_EXCHANGES + 1, MAX_EXCHANGES + EXCESS_EXCHANGES)));
  }

  @Test
  void whenServletInvoked_responseDisplaysRecentExchanges() throws IOException {
    IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addTestExchange);

    servlet.doGet(request, response);

    assertThat(response.getHtml(), containsString("request 4"));
  }
}
