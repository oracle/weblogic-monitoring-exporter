// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.oracle.wls.exporter.ConfigurationUpdaterStub;
import com.oracle.wls.exporter.InMemoryFileSystem;
import com.oracle.wls.exporter.LiveConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.InMemoryFileSystem.withNoParams;
import static com.oracle.wls.exporter.WebAppConstants.CONFIGURATION_PAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Russell Gold
 */
class MainServletTest {

    private final MainServlet servlet = new MainServlet();
    private final HttpServletRequestStub request = HttpServletRequestStub.createGetRequest();
    private final HttpServletResponseStub response = HttpServletResponseStub.createServletResponse();

    @BeforeEach
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        ConfigurationUpdaterStub.install();
        LiveConfiguration.loadFromString("");
        request.setContextPath("/exporter");
    }

    @AfterEach
    public void tearDown() {
        InMemoryFileSystem.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    void landingPage_isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    void servlet_hasWebServletAnnotation() {
        assertThat(MainServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    void servletAnnotationIndicatesMainPage() {
        WebServlet annotation = MainServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/"));
    }

    @Test
    void whenServletPathIsSlash_showSimpleLinkToMetrics() throws Exception {
        request.setServletPath("/");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("href=\"metrics\""));
    }

    @Test
    void whenServletPathIsEmpty_showFullLinkToMetrics() throws Exception {
        request.setServletPath("");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("href=\"/exporter/metrics\""));
    }

    @Test
    void getRequest_containsConfigurationForm() throws Exception {
        request.setServletPath("/");
        servlet.doGet(request, response);

        assertThat(response.getHtml(),
                containsString("form action=\"" + CONFIGURATION_PAGE + "\" method=\"post\" enctype=\"multipart/form-data\""));
    }

    @Test
    void whenServletPathIsEmpty_showFullPathToConfigurationServlet() throws Exception {
        request.setServletPath("");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("action=\"/exporter/" + CONFIGURATION_PAGE + "\""));
    }

    @Test
    void getRequestShowsCurrentConfiguration() throws Exception {
        InMemoryFileSystem.defineResource(ServletUtils.CONFIG_YML, PARSED_CONFIGURATION);
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(EFFECTIVE_CONFIGURATION));
    }

    @Test
    void whenNewConfigAvailable_getRequestShowsNewConfiguration() throws Exception {
        InMemoryFileSystem.defineResource(ServletUtils.CONFIG_YML, EMPTY_CONFIGURATION);
        servlet.init(withNoParams());

        ConfigurationUpdaterStub.newConfiguration(1, PARSED_CONFIGURATION);
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(EFFECTIVE_CONFIGURATION));
    }

    @Test
    void whenNewConfigHasNoQueries_displayEmptyConfiguration() throws Exception {
        InMemoryFileSystem.defineResource(ServletUtils.CONFIG_YML, PARSED_CONFIGURATION);
        servlet.init(withNoParams());

        ConfigurationUpdaterStub.newConfiguration(1, "queries:\n");
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(EMPTY_CONFIGURATION));
    }

    private static final String EMPTY_CONFIGURATION =
            "hostName: " + HttpServletRequestStub.HOST_NAME + "\n" +
            "port: " + HttpServletRequestStub.PORT + "\n";

    private static final String PARSED_CONFIGURATION =
            "hostName: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n" +
            "    stringValues:\n" +
            "      colors: [red, green, blue]";

    private static final String EFFECTIVE_CONFIGURATION =
            "hostName: " + HttpServletRequestStub.HOST_NAME + "\n" +
            "port: " + HttpServletRequestStub.PORT + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n" +
            "    stringValues:\n" +
            "      colors: [red, green, blue]";

// assertThat(getPage(), containsForm().withAction(CONFIGURATION_ACTION).withMethod("post").withEncType("multipart/form-data")
    // assertThat(getPage(), containsForm().withRadioButton("effect").withValues("append","replace").withDefault("append")
    //
}
