// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.oracle.wls.exporter.ClockStub;
import com.oracle.wls.exporter.ExporterQueries;
import com.oracle.wls.exporter.ExporterQuery;
import com.oracle.wls.exporter.WebClientFactoryStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.oracle.wls.exporter.webapp.HttpServletRequestStub.createGetRequest;
import static com.oracle.wls.exporter.webapp.HttpServletResponseStub.createServletResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class QueriesServletTest {

    private final Instant startInstant = Instant.parse("2024-05-15T18:35:24.00Z");
    private final WebClientFactoryStub factory = new WebClientFactoryStub();
    private final QueriesServlet servlet = new QueriesServlet(factory);
    private final HttpServletRequestStub request = createGetRequest();
    private final HttpServletResponseStub response = createServletResponse();
    private final ClockStub clock = createStrictStub(ClockStub.class);
    private final List<Memento> mementos = new ArrayList<>();

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        mementos.add(StaticStubSupport.install(ExporterQuery.class, "clock", clock));

        clock.setCurrentMsec(startInstant.toEpochMilli());
        ExporterQueries.clear();
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    void isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    void servlet_isPublic() {
        assertThat(Modifier.isPublic(MessagesServlet.class.getModifiers()), is(true));
    }

    @Test
    void servlet_hasWebServletAnnotation() {
        assertThat(QueriesServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    void servletAnnotationIndicatesMetricsPage() {
        WebServlet annotation = QueriesServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/queries"));
    }

    @Test
    void afterQueriesAdded_servletResponseContainsData() throws ServletException, IOException {
        ExporterQueries.addQuery(createGetRequest().withRemoteHost("host1"));
        ExporterQueries.addQuery(createGetRequest().withRemoteHost("host2"));

        servlet.doGet(request, response);

        assertThat(response.getHtml(),
                stringContainsInOrder("Request ", " from host1", "Request ", " from host2"));
    }
}
