// Copyright (c) 2017, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.google.common.collect.ImmutableMap;
import com.oracle.wls.exporter.webapp.ExporterServlet;
import com.oracle.wls.exporter.webapp.HttpServletRequestStub;
import com.oracle.wls.exporter.webapp.HttpServletResponseStub;
import com.oracle.wls.exporter.webapp.ServletUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.oracle.wls.exporter.InMemoryFileSystem.withNoParams;
import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_CHALLENGE_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.matchers.CommentsOnlyMatcher.containsOnlyComments;
import static com.oracle.wls.exporter.matchers.MetricsNamesSnakeCaseMatcher.usesSnakeCase;
import static com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static com.oracle.wls.exporter.matchers.ResponseHeaderMatcher.containsHeader;
import static com.oracle.wls.exporter.webapp.HttpServletRequestStub.createGetRequest;
import static com.oracle.wls.exporter.webapp.HttpServletResponseStub.createServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Russell Gold
 */
class ExporterServletTest {
    private static final String localHostName = ServletInvocationContext.getLocalHostName();
    private static final int REST_PORT = 7654;
    private static final int LOCAL_PORT = 7001;
    private static final String WLS_HOST = "myhost";
    private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
    private static final String SECURE_URL_PATTERN = "https://%s:%d/management/weblogic/latest/serverRuntime/search";
    private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String TWO_VALUE_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]";
    private static final String REST_PORT_CONFIG = "restPort: " + REST_PORT +
                                                   "\nqueries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String CONFIG_WITH_CATEGORY_VALUE = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1, testSample2, bogus]";
    private static final String CONFIG_WITH_STRING_VALUES = "queries:" +
            "\n- groups:\n    key: name\n    stringValues:\n      color: [red, green]\n      size: [tall, grande, venti]";
    private static final String MULTI_QUERY_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]" +
            "\n- colors:                         \n    key: hue \n    values: wavelength";
    private final WebClientFactoryStub factory = new WebClientFactoryStub();
    private final ExporterServlet servlet = new ExporterServlet(factory);
    private final HttpServletRequestStub request = createGetRequest().withLocalHostName(WLS_HOST).withLocalPort(LOCAL_PORT);
    private final HttpServletResponseStub response = createServletResponse();
    private Locale locale;

    @BeforeEach
    public void setUp() throws Exception {
        locale = Locale.getDefault();
        InMemoryFileSystem.install();
        ConfigurationUpdaterStub.install();
        LiveConfiguration.setServer(WLS_HOST, LOCAL_PORT);
        WlsRestExchanges.clear();
        UrlBuilder.clearHistory();
    }

    @AfterEach
    public void tearDown() {
        Locale.setDefault(locale);
        InMemoryFileSystem.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    void exporter_isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    void servlet_hasWebServletAnnotation() {
        assertThat(ExporterServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    void servletAnnotationIndicatesMetricsPage() {
        WebServlet annotation = ExporterServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/metrics"));
    }

    @Test
    void whenConfigParamNotFound_configurationHasNoQueries() throws Exception {
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(LiveConfiguration.hasQueries(), is(false));
    }

    @Test
    void whenConfigFileNameNotAbsolute_getReportsTheIssue() throws Exception {
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("# No configuration"));
    }

    @Test
    void whenConfigFileNotFound_getReportsTheIssue() throws Exception {
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("# No configuration"));
    }

    @Test
    void onPlaintextGet_defineConnectionUrlFromContext() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);
        
        assertThat(factory.getClientUrl(),
                   equalTo(String.format(URL_PATTERN, request.getLocalName(), LOCAL_PORT)));
    }

    @Test
    void onSecurePlaintextGet_defineConnectionUrlFromContext() throws Exception {
        initServlet(ONE_VALUE_CONFIG);
        request.setSecure(true);

        servlet.doGet(request, response);

        assertThat(factory.getClientUrl(),
                   equalTo(String.format(SECURE_URL_PATTERN, request.getLocalName(), LOCAL_PORT)));
    }

    @Test
    void whenRestPortDefined_connectionUrlUsesRestPort() throws IOException {
        initServlet(REST_PORT_CONFIG);

        servlet.doGet(request, response);

        assertThat(factory.getClientUrl(),  equalTo(String.format(URL_PATTERN, request.getLocalName(), REST_PORT)));
    }

    @Test
    void whenRestPortAccessFails_switchToLocalPort() throws IOException {
        initServlet(REST_PORT_CONFIG);
        factory.throwConnectionFailure(request.getLocalName(), REST_PORT);
        factory.addJsonResponse(new HashMap<>());

        servlet.doGet(request, response);

        assertThat(factory.getClientUrl(),  equalTo(String.format(URL_PATTERN, request.getLocalName(), LOCAL_PORT)));
    }

    @Test
    void whenRequestHostNameAccessFails_switchToLocalhost() throws IOException {
        request.withLocalHostName("inaccessibleServer");
        initServlet(ONE_VALUE_CONFIG);
        factory.throwConnectionFailure("inaccessibleServer", LOCAL_PORT);
        factory.addJsonResponse(new HashMap<>());

        servlet.doGet(request, response);

        assertThat(factory.getClientUrl(),  equalTo(String.format(URL_PATTERN, localHostName, LOCAL_PORT)));
    }

    @Test
    void whenServerSends403StatusOnGet_returnToClient() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        factory.reportNotAuthorized();
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(SC_FORBIDDEN));
    }

    @Test
    void whenServerSends400StatusOnGet_reportErrorInComments() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        factory.reportBadQuery();
        servlet.doGet(request, response);

        assertThat(toHtml(response), followsPrometheusRules());
    }

    @Test
    void whenServerSends401StatusOnGet_returnToClient() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        factory.reportAuthenticationRequired("Test-Realm");
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(response, containsHeader(AUTHENTICATION_CHALLENGE_HEADER, "Basic realm=\"Test-Realm\""));
    }

    @Test
    void whenClientSendsAuthenticationHeader_passToServer() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        request.setHeader(AUTHENTICATION_HEADER, "auth-credentials");
        servlet.doGet(request, response);

        assertThat(factory.getSentAuthentication(), equalTo("auth-credentials"));
    }

    @Test
    void onGet_sendJsonQuery() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(factory.getSentQuery(),
                   hasJsonPath("$.children.groups.fields", contains("name", "testSample1")));
    }

    private void initServlet(String configuration) {
        InMemoryFileSystem.defineResource(ServletUtils.CONFIG_YML, configuration);
        servlet.init(withNoParams());
    }

    @Test
    void onGet_recordJsonQuery() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);

      assertThat(WlsRestExchanges.getExchanges(), not(empty()));
    }

    @Test
    void onGet_sendXRequestedHeader() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(factory.getSentHeaders("X-Requested-By"), not(empty()));
    }

    @Test
    void onGet_displayMetrics() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(TWO_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("groupValue_testSample1{name=\"first\"} 12"));
        assertThat(toHtml(response), containsString("groupValue_testSample1{name=\"second\"} -3"));
        assertThat(toHtml(response), containsString("groupValue_testSample2{name=\"second\"} 71.0"));
    }

    private String toHtml(HttpServletResponseStub response) {
        return response.getHtml();
    }

    private Map<String,Object> getGroupResponseMap() {
        return ImmutableMap.of("groups", new ItemHolder(
                    ImmutableMap.of("name", "first", "testSample1", 12, "testSample2", 12.3, "bogus", "red"),
                    ImmutableMap.of("name", "second", "testSample1", -3, "testSample2", 71.0),
                    ImmutableMap.of("name", "third", "testSample1", 85, "testSample2", 65.8)
        ));
    }

    @Test
    void whenNewConfigAvailable_loadBeforeGeneratingMetrics() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(ONE_VALUE_CONFIG);
        ConfigurationUpdaterStub.newConfiguration(1, TWO_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("groupValue_testSample2{name=\"second\"} 71.0"));
    }

    @Test
    void onGet_displayMetricsInSnakeCase() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet("metricsNameSnakeCase: true\nqueries:\n- groups:\n" +
                "    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]");

        servlet.doGet(request, this.response);

        assertThat(toHtml(this.response), usesSnakeCase());
    }

    @Test
    void onGet_metricsArePrometheusCompliant() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(CONFIG_WITH_CATEGORY_VALUE);

        servlet.doGet(request, response);

        assertThat(toHtml(response), followsPrometheusRules());
    }

    @Test
    void onGet_producePerformanceMetrics() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(CONFIG_WITH_CATEGORY_VALUE);

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("wls_scrape_mbeans_count_total"));
    }

    @Test
    void onGetWithMultipleQueries_displayMetrics() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        factory.addJsonResponse(getColorResponseMap());
        initServlet(MULTI_QUERY_CONFIG);

        servlet.doGet(request, this.response);

        assertThat(toHtml(this.response), containsString("groupValue_testSample1{name=\"first\"} 12"));
        assertThat(toHtml(this.response), containsString("groupValue_testSample1{name=\"second\"} -3"));
        assertThat(toHtml(this.response), containsString("wavelength{hue=\"green\"} 540"));
    }

    private Map<String,Object> getColorResponseMap() {
        return ImmutableMap.of("colors", new ItemHolder(
                    ImmutableMap.of("hue", "red", "wavelength", 700),
                    ImmutableMap.of("hue", "green", "wavelength", 540),
                    ImmutableMap.of("hue", "blue", "wavelength", 475)
        ));
    }

    @Test
    void onGetWithStringValues_displayMetrics() throws Exception {
        factory.addJsonResponse(getStringResponseMap());
        initServlet(CONFIG_WITH_STRING_VALUES);

        servlet.doGet(request, this.response);

        assertThat(toHtml(this.response), containsString("color{name=\"fred\",value=\"red\"} 0"));
        assertThat(toHtml(this.response), containsString("color{name=\"george\",value=\"green\"} 1"));
        assertThat(toHtml(this.response), containsString("color{name=\"ron\",value=\"blue\"} -1"));
        assertThat(toHtml(this.response), containsString("size{name=\"fred\",value=\"tall\"} 0"));
        assertThat(toHtml(this.response), containsString("size{name=\"george\",value=\"grande\"} 1"));
        assertThat(toHtml(this.response), containsString("size{name=\"ron\",value=\"venti\"} 2"));
    }

    private Map<String,Object> getStringResponseMap() {
        return ImmutableMap.of("groups", new ItemHolder(
                    ImmutableMap.of("name", "fred", "color", "red", "size", "tall"),
                    ImmutableMap.of("name", "george", "color", "green", "size", "grande"),
                    ImmutableMap.of("name", "ron", "color", "blue", "size", "venti")
        ));
    }

    @Test
    void whenNoQueries_produceNoOutput() throws Exception {
        initServlet("");

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsOnlyComments());
    }

    @Test
    void whenNoConfiguration_produceNoOutput() throws Exception {
        servlet.doGet(request, response);

        assertThat(toHtml(response), containsOnlyComments());
    }

    @Test
    void whenHttpConnectionFails_produceConnectionWarning() throws Exception {
        initServlet(CONFIG_WITH_CATEGORY_VALUE);
        factory.throwConnectionFailure(WLS_HOST, LOCAL_PORT);
        factory.throwConnectionFailure("localhost", LOCAL_PORT);

        servlet.doGet(request, response);

        assertThat(toHtml(response), allOf(
              containsOnlyComments(),
              containsString("restPort"),
              containsString("restHostName"),
              containsString("http://" + WLS_HOST + ':' + LOCAL_PORT)));
    }

    @Test
    void whenKeyAlsoListedAsValue_dontDisplayIt() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet("queries:" +
                "\n- groups:\n    prefix: groupValue_\n    key: testSample1\n    values: [testSample1]");

        servlet.doGet(request, response);

        assertThat(toHtml(this.response), not(containsString("groupValue_testSample1{testSample1")));
    }

    @Test
    void whenSessionActiveDuringGet_invalidateOnExit() throws Exception {
        request.getSession(true);

        servlet.doGet(request, response);

        assertThat(request.hasInvalidatedSession(), is(true));
    }
}
