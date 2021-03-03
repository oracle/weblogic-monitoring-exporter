// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.oracle.wls.exporter.ErrorLog;
import com.oracle.wls.exporter.InMemoryFileSystem;
import com.oracle.wls.exporter.LiveConfiguration;
import com.oracle.wls.exporter.WebClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

public class LogServletTest {

    private final LogServlet servlet = new LogServlet();
    private final HttpServletRequestStub request = HttpServletRequestStub.createGetRequest();
    private final HttpServletResponseStub response = HttpServletResponseStub.createServletResponse();
    private final List<Memento> mementos = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        LiveConfiguration.loadFromString("");
    }

    @AfterEach
    public void tearDown() {
        mementos.forEach(Memento::revert);
        InMemoryFileSystem.uninstall();
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
    public void whenNoErrorsReported_saySo() throws IOException {
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("No errors reported."));
    }

    @Test
    public void whenErrorsReported_listThem() throws NoSuchFieldException, IOException {
        ErrorLog errorLog = new ErrorLog();
        mementos.add(StaticStubSupport.install(LiveConfiguration.class, "errorLog", errorLog));

        errorLog.log(new WebClientException("Unable to reach server"));
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("WebClientException: Unable to reach server"));
    }
}
