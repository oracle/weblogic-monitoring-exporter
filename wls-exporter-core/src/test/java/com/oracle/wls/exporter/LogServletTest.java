// Copyright (c) 2019, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class LogServletTest {

    private final LogServlet servlet = new LogServlet();
    private final HttpServletRequestStub request = HttpServletRequestStub.createGetRequest();
    private final HttpServletResponseStub response = HttpServletResponseStub.createServletResponse();
    private final List<Memento> mementos = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        LiveConfiguration.loadFromString("");
    }

    @After
    public void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    public void landingPage_isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_hasWebServletAnnotation() {
        assertThat(LogServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesMainPage() {
        WebServlet annotation = LogServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/log"));
    }

    @Test
    public void whenNoErrorsReported_saySo() throws ServletException, IOException {
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("No errors reported."));
    }

    @Test
    public void whenErrorsReported_listThem() throws NoSuchFieldException, ServletException, IOException {
        ErrorLog errorLog = new ErrorLog();
        mementos.add(StaticStubSupport.install(LiveConfiguration.class, "errorLog", errorLog));

        errorLog.log(new WebClientException("Unable to reach server"));
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("WebClientException: Unable to reach server"));
    }
}
