// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package io.prometheus.wls.rest;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.stream.IntStream;

import static io.prometheus.wls.rest.MessagesServlet.MAX_EXCHANGES;
import static io.prometheus.wls.rest.HttpServletRequestStub.createGetRequest;
import static io.prometheus.wls.rest.HttpServletResponseStub.createServletResponse;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class MessagesServletTest {
    private static final int EXCESS_EXCHANGES = 3;
    private MessagesServlet servlet = new MessagesServlet();
    private HttpServletRequestStub request = createGetRequest();
    private HttpServletResponseStub response = createServletResponse();

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
        MessagesServlet.addExchange("request 1", "response1");

        assertThat(MessagesServlet.getMessages(), hasItem(both(containsString("request 1")).and(containsString("response1"))));
    }

    @Test
    public void afterMaximumExchangesAdded_retrieveAllExchanges() {
        IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addTestExchange);

        assertThat(MessagesServlet.getMessages(), contains(getExchangeMatchers(1, MAX_EXCHANGES)));
    }

    private void addTestExchange(int i) {
        MessagesServlet.addExchange("request " + i, "response " + i);
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
    public void whenServletInvoked_responseDisplaysRecentExchanges() throws ServletException, IOException {
        IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addTestExchange);

        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("request 4"));
    }
}
