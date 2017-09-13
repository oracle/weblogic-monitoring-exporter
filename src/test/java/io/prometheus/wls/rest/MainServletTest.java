package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import static io.prometheus.wls.rest.InMemoryFileSystem.withNoParams;
import static io.prometheus.wls.rest.ServletConstants.CONFIGURATION_ACTION;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class MainServletTest {

    private MainServlet servlet = new MainServlet();
    private HttpServletRequestStub request = HttpServletRequestStub.createGetRequest();
    private HttpServletResponseStub response = HttpServletResponseStub.createServletResponse();

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        LiveConfiguration.loadFromString("");
        request.setContextPath("/exporter");
    }

    @After
    public void tearDown() throws Exception {
        InMemoryFileSystem.uninstall();
    }

    @Test
    public void landingPage_isHttpServlet() throws Exception {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_hasWebServletAnnotation() throws Exception {
        assertThat(MainServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesMainPage() throws Exception {
        WebServlet annotation = MainServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/"));
    }

    @Test
    public void whenServletPathIsSlash_showSimpleLinkToMetrics() throws Exception {
        request.setServletPath("/");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("href=\"metrics\""));
    }

    @Test
    public void whenServletPathIsEmpty_showFullLinkToMetrics() throws Exception {
        request.setServletPath("");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("href=\"/exporter/metrics\""));
    }

    @Test
    public void getRequest_containsConfigurationForm() throws Exception {
        request.setServletPath("/");
        servlet.doGet(request, response);

        assertThat(response.getHtml(),
                containsString("form action=\"" + CONFIGURATION_ACTION + "\" method=\"post\" enctype=\"multipart/form-data\""));
    }

    @Test
    public void whenServletPathIsEmpty_showFullPathToConfigurationServlet() throws Exception {
        request.setServletPath("");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("action=\"/exporter/" + CONFIGURATION_ACTION + "\""));
    }

    @Test
    public void getRequestShowsCurrentConfiguration() throws Exception {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, PARSED_CONFIGURATION);
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(EFFECTIVE_CONFIGURATION));
    }

    private static final String PARSED_CONFIGURATION =
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String EFFECTIVE_CONFIGURATION =
            "host: " + HttpServletRequestStub.HOST + "\n" +
            "port: " + HttpServletRequestStub.PORT + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

// assertThat(getPage(), containsForm().withAction(CONFIGURATION_ACTION).withMethod("post").withEncType("multipart/form-data")
    // assertThat(getPage(), containsForm().withRadioButton("effect").withValues("append","replace").withDefault("append")
    //
}
