// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.wls.exporter.InMemoryFileSystem.withNoParams;
import static com.oracle.wls.exporter.ServletConstants.CONFIGURATION_PAGE;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class MainServletTest {

    private final MainServlet servlet = new MainServlet();
    private final HttpServletRequestStub request = HttpServletRequestStub.createGetRequest();
    private final HttpServletResponseStub response = HttpServletResponseStub.createServletResponse();

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        ConfigurationUpdaterStub.install();
        LiveConfiguration.loadFromString("");
        request.setContextPath("/exporter");
    }

    @After
    public void tearDown() {
        InMemoryFileSystem.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    public void landingPage_isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_hasWebServletAnnotation() {
        assertThat(MainServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesMainPage() {
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
                containsString("form action=\"" + CONFIGURATION_PAGE + "\" method=\"post\" enctype=\"multipart/form-data\""));
    }

    @Test
    public void whenServletPathIsEmpty_showFullPathToConfigurationServlet() throws Exception {
        request.setServletPath("");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("action=\"/exporter/" + CONFIGURATION_PAGE + "\""));
    }

    @Test
    public void getRequestShowsCurrentConfiguration() throws Exception {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, PARSED_CONFIGURATION);
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(EFFECTIVE_CONFIGURATION));
    }

    @Test
    public void whenNewConfigAvailable_getRequestShowsNewConfiguration() throws Exception {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, EMPTY_CONFIGURATION);
        servlet.init(withNoParams());

        ConfigurationUpdaterStub.newConfiguration(1, PARSED_CONFIGURATION);
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(EFFECTIVE_CONFIGURATION));
    }

    @Test
    public void whenNewConfigHasNoQueries_displayEmptyConfiguration() throws Exception {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, PARSED_CONFIGURATION);
        servlet.init(withNoParams());

        ConfigurationUpdaterStub.newConfiguration(1, "queries:\n");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(EMPTY_CONFIGURATION));
    }

    private static final String EMPTY_CONFIGURATION =
            "host: " + HttpServletRequestStub.HOST + "\n" +
            "port: " + HttpServletRequestStub.PORT + "\n";

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
