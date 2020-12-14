// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.wls.exporter.HttpServletRequestStub.LOCAL_PORT;
import static com.oracle.wls.exporter.HttpServletRequestStub.createGetRequest;
import static com.oracle.wls.exporter.HttpServletResponseStub.createServletResponse;
import static com.oracle.wls.exporter.InMemoryFileSystem.withNoParams;
import static com.oracle.wls.exporter.ServletConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.ServletConstants.COOKIE_HEADER;
import static com.oracle.wls.exporter.domain.JsonPathMatcher.hasJsonPath;
import static com.oracle.wls.exporter.matchers.CommentsOnlyMatcher.containsOnlyComments;
import static com.oracle.wls.exporter.matchers.MetricsNamesSnakeCaseMatcher.usesSnakeCase;
import static com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static com.oracle.wls.exporter.matchers.ResponseHeaderMatcher.containsHeader;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class ExporterServletTest {
    private static final int REST_PORT = 7654;
    private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
    private static final String SECURE_URL_PATTERN = "https://%s:%d/management/weblogic/latest/serverRuntime/search";
    private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String TWO_VALUE_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]";
    private static final String REST_PORT_CONFIG = "restPort: " + REST_PORT +
                                                   "\nqueries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String CONFIG_WITH_CATEGORY_VALUE = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1, testSample2, bogus]";
    private static final String MULTI_QUERY_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]" +
            "\n- colors:                         \n    key: hue \n    values: wavelength";
    private final WebClientFactoryStub factory = new WebClientFactoryStub();
    private final ExporterServlet servlet = new ExporterServlet(factory);
    private final HttpServletRequestStub request = createGetRequest();
    private final HttpServletResponseStub response = createServletResponse();
    private Locale locale;

    @Before
    public void setUp() throws Exception {
        locale = Locale.getDefault();
        InMemoryFileSystem.install();
        ConfigurationUpdaterStub.install();
        LiveConfiguration.loadFromString("");
        ExporterSession.cacheSession(null, null);
        MessagesServlet.clear();
        UrlBuilder.clearHistory();
    }

    @After
    public void tearDown() {
        Locale.setDefault(locale);
        InMemoryFileSystem.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    public void exporter_isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_hasWebServletAnnotation() {
        assertThat(ExporterServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesMetricsPage() {
        WebServlet annotation = ExporterServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/metrics"));
    }

    @Test
    public void whenConfigParamNotFound_configurationHasNoQueries() throws Exception {
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(LiveConfiguration.hasQueries(), is(false));
    }

    @Test
    public void whenConfigFileNameNotAbsolute_getReportsTheIssue() throws Exception {
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("# No configuration"));
    }

    @Test
    public void whenConfigFileNotFound_getReportsTheIssue() throws Exception {
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("# No configuration"));
    }

    @Test
    public void onPlaintextGet_defineConnectionUrlFromContext() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);
        
        assertThat(factory.getClientUrl(),
                   equalTo(String.format(URL_PATTERN, request.getLocalName(), HttpServletRequestStub.LOCAL_PORT)));
    }

    @Test
    public void onSecurePlaintextGet_defineConnectionUrlFromContext() throws Exception {
        initServlet(ONE_VALUE_CONFIG);
        request.setSecure(true);

        servlet.doGet(request, response);

        assertThat(factory.getClientUrl(),
                   equalTo(String.format(SECURE_URL_PATTERN, request.getLocalName(), HttpServletRequestStub.LOCAL_PORT)));
    }

    @Test
    public void whenRestPortDefined_connectionUrlUsesRestPort() throws ServletException, IOException {
        initServlet(REST_PORT_CONFIG);

        servlet.doGet(request, response);

        assertThat(factory.getClientUrl(),  equalTo(String.format(URL_PATTERN, request.getLocalName(), REST_PORT)));
    }


    @Test
    public void whenRestPortAccessFails_switchToLocalPort() throws ServletException, IOException {
        initServlet(REST_PORT_CONFIG);
        factory.throwConnectionFailure("localhost", REST_PORT);
        factory.addJsonResponse(new HashMap<>());

        servlet.doGet(request, response);

        assertThat(factory.getClientUrl(),  equalTo(String.format(URL_PATTERN, request.getLocalName(), LOCAL_PORT)));
    }

    @Test
    public void whenServerSends403StatusOnGet_returnToClient() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        factory.reportNotAuthorized();
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(SC_FORBIDDEN));
    }

    @Test
    public void whenServerSends400StatusOnGet_reportErrorInComments() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        factory.reportBadQuery();
        servlet.doGet(request, response);

        assertThat(toHtml(response), followsPrometheusRules());
    }

    @Test
    public void whenServerSends401StatusOnGet_returnToClient() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        factory.reportAuthenticationRequired("Test-Realm");
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(response, containsHeader("WWW-Authenticate", "Basic realm=\"Test-Realm\""));
    }

    @Test
    public void whenServerSendsSetCookieHeader_returnToClient() throws Exception {
        final String SET_COOKIE_HEADER = "ACookie=AValue; Secure";

        factory.setSetCookieResponseHeader(SET_COOKIE_HEADER);
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(TWO_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(response, containsHeader("Set-Cookie", SET_COOKIE_HEADER));
    }

    @Test
    public void whenClientSendsAuthenticationHeader_passToServer() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        request.setHeader(AUTHENTICATION_HEADER, "auth-credentials");
        request.setHeader(COOKIE_HEADER, "a=2; JSESSIONID=abc#$");
        servlet.doGet(request, response);

        assertThat(factory.getSentAuthentication(), equalTo("auth-credentials"));
    }

    @Test
    public void whenClientSendsAuthenticationMatchingSession_passSessionToServer() throws Exception {
        final String SESSION_COOKIE = ExporterSession.SESSION_COOKIE_PREFIX + "abcdef";
        ExporterSession.cacheSession("auth-credentials", SESSION_COOKIE);
        initServlet(ONE_VALUE_CONFIG);

        request.setHeader(AUTHENTICATION_HEADER, "auth-credentials");
        request.setHeader(COOKIE_HEADER, "a=2; b=abc#$");
        servlet.doGet(request, response);

        assertThat(factory.getSentSessionCookie(), equalTo(SESSION_COOKIE));
    }

    @Test
    public void whenServerSendsSetCookieHeader_establishExporterSession() throws Exception {
        final String SESSION_COOKIE = ExporterSession.SESSION_COOKIE_PREFIX + "abc@345";
        final String CREDENTIALS = "auth-credentials";

        request.setHeader(AUTHENTICATION_HEADER, CREDENTIALS);
        factory.setSetCookieResponseHeader(SESSION_COOKIE);
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(TWO_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(ExporterSession.getSessionCookie(), equalTo(SESSION_COOKIE));
        assertThat(ExporterSession.getAuthentication(), equalTo(CREDENTIALS));
    }

    // todo whenAuthorization header and already have matching session cookie, pass to web client
    // todo consecutive requests for same auth header use same session cookie

    @Test
    public void whenClientSendsCookieHeaderOnGet_passToServer() throws Exception {
        final String SESSION_COOKIE = ExporterSession.SESSION_COOKIE_PREFIX + "with-chocolate-chips";
        initServlet(ONE_VALUE_CONFIG);

        request.setHeader("Cookie", SESSION_COOKIE);
        servlet.doGet(request, response);

        assertThat(factory.getSentSessionCookie(), equalTo(SESSION_COOKIE));
    }

    @Test
    public void onGet_sendJsonQuery() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(factory.getSentQuery(),
                   hasJsonPath("$.children.groups.fields").withValues("name", "testSample1"));
    }

    private void initServlet(String configuration) {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, configuration);
        servlet.init(withNoParams());
    }

    @Test
    public void onGet_recordJsonQuery() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(MessagesServlet.getMessages(), not(empty()));
    }

    @Test
    public void onGet_sendXRequestedHeader() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(factory.getSentHeaders(), hasKey("X-Requested-By"));
    }

    @Test
    public void onGet_displayMetrics() throws Exception {
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
    public void whenNewConfigAvailable_loadBeforeGeneratingMetrics() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(ONE_VALUE_CONFIG);
        ConfigurationUpdaterStub.newConfiguration(1, TWO_VALUE_CONFIG);

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("groupValue_testSample2{name=\"second\"} 71.0"));
    }

    @Test
    public void onGet_displayMetricsInSnakeCase() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet("metricsNameSnakeCase: true\nqueries:\n- groups:\n" +
                "    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]");

        servlet.doGet(request, this.response);

        assertThat(toHtml(this.response), usesSnakeCase());
    }

    @Test
    public void onGet_metricsArePrometheusCompliant() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(CONFIG_WITH_CATEGORY_VALUE);

        servlet.doGet(request, response);

        assertThat(toHtml(response), followsPrometheusRules());
    }

    @Test
    public void onGet_producePerformanceMetrics() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(CONFIG_WITH_CATEGORY_VALUE);

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("wls_scrape_mbeans_count_total{instance=\"myhost:7654\"} 6"));
    }

    @Test
    public void onGetInForeignLocale_performanceMetricsUsePeriodForFloatingPoint() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet(CONFIG_WITH_CATEGORY_VALUE);

        Locale.setDefault(Locale.FRANCE);
        servlet.doGet(request, response);

        assertThat(getMetricValue("wls_scrape_duration_seconds"), containsString("."));
    }

    @SuppressWarnings("SameParameterValue")
    private String getMetricValue(String metricsName) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new StringReader(toHtml(response)));
        do {
            line = reader.readLine();
        } while (line != null && !line.contains(metricsName));

        return (line == null) ? "" : line.split(" ")[1];
    }

    @Test
    public void onGetWithMultipleQueries_displayMetrics() throws Exception {
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
    public void whenNoQueries_produceNoOutput() throws Exception {
        initServlet("");

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsOnlyComments());
    }

    @Test
    public void whenNoConfiguration_produceNoOutput() throws Exception {
        servlet.doGet(request, response);

        assertThat(toHtml(response), containsOnlyComments());
    }

    @Test
    public void whenHttpConnectionFails_produceConnectionWarning() throws Exception {
        initServlet(CONFIG_WITH_CATEGORY_VALUE);
        factory.throwConnectionFailure("myhost", 1234);

        servlet.doGet(request, response);

        assertThat(toHtml(response), allOf(containsOnlyComments(),containsString("restPort"), containsString("http://myhost:1234")));
    }

    @Test
    public void whenKeyAlsoListedAsValue_dontDisplayIt() throws Exception {
        factory.addJsonResponse(getGroupResponseMap());
        initServlet("queries:" +
                "\n- groups:\n    prefix: groupValue_\n    key: testSample1\n    values: [testSample1]");

        servlet.doGet(request, response);

        assertThat(toHtml(this.response), not(containsString("groupValue_testSample1{testSample1")));
    }

    @Test
    public void whenSessionActiveDuringGet_invalidateOnExit() throws Exception {
        request.getSession(true);

        servlet.doGet(request, response);

        assertThat(request.hasInvalidatedSession(), is(true));
    }




    abstract static class ServletInputStreamStub extends ServletInputStream {
        private final InputStream inputStream;

        public ServletInputStreamStub(String contents) {
            inputStream = new ByteArrayInputStream(contents.getBytes());
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }

}
