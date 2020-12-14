// Copyright (c) 2019, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.stream.IntStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.wls.exporter.HttpServletRequestStub.createGetRequest;
import static com.oracle.wls.exporter.HttpServletResponseStub.createServletResponse;
import static com.oracle.wls.exporter.MessagesServlet.MAX_EXCHANGES;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class MessagesServletTest {
    private static final int EXCESS_EXCHANGES = 3;
    private static final String URL = "http://localhost:7001";
    private final MessagesServlet servlet = new MessagesServlet();
    private final HttpServletRequestStub request = createGetRequest();
    private final HttpServletResponseStub response = createServletResponse();

    @Before
    public void setUp() {
        MessagesServlet.clear();
    }

    @Test
    public void diagnostics_isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_isPublic() {
        assertThat(Modifier.isPublic(MessagesServlet.class.getModifiers()), is(true));
    }

    @Test
    public void servlet_hasWebServletAnnotation() {
        assertThat(MessagesServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesMetricsPage() {
        WebServlet annotation = MessagesServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/messages"));
    }

    @Test
    public void afterExchangeAdded_retrieveExchange() {
        MessagesServlet.addExchange(URL, "request 1", "response1");

        assertThat(MessagesServlet.getMessages(), hasItem(both(containsString("request 1")).and(containsString("response1"))));
    }

    @Test
    public void afterMaximumExchangesAdded_retrieveAllExchanges() {
        IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addTestExchange);

        assertThat(MessagesServlet.getMessages(), contains(getExchangeMatchers(1, MAX_EXCHANGES)));
    }

    private void addTestExchange(int i) {
        MessagesServlet.addExchange(URL, "request " + i, "response " + i);
    }

    @SuppressWarnings("unchecked")
    private Matcher<String>[] getExchangeMatchers(int first, int last) {
        return IntStream.rangeClosed(first, last).mapToObj(this::getExchangeMatcher).toArray(Matcher[]::new);
    }

    private Matcher<String> getExchangeMatcher(int i)  {
        return containsString("request " + i);
    }

    @Test
    public void afterMoreThanMaximumExchangesAdded_retrieveLastMExchanges() {
        IntStream.rangeClosed(1, MAX_EXCHANGES+EXCESS_EXCHANGES).forEach(this::addTestExchange);

        assertThat(MessagesServlet.getMessages(), contains(getExchangeMatchers(EXCESS_EXCHANGES+1, MAX_EXCHANGES+EXCESS_EXCHANGES)));
    }

    @Test
    public void whenServletInvoked_responseDisplaysRecentExchanges() throws IOException {
        IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addTestExchange);

        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("request 4"));
    }
}
