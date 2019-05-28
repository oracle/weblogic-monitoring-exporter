package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.io.*;
import java.util.*;

import static com.meterware.simplestub.Stub.createStrictStub;
import static io.prometheus.wls.rest.HttpServletRequestStub.createGetRequest;
import static io.prometheus.wls.rest.HttpServletResponseStub.createServletResponse;
import static io.prometheus.wls.rest.InMemoryFileSystem.withNoParams;
import static io.prometheus.wls.rest.ServletConstants.AUTHENTICATION_HEADER;
import static io.prometheus.wls.rest.ServletConstants.COOKIE_HEADER;
import static io.prometheus.wls.rest.domain.JsonPathMatcher.hasJsonPath;
import static io.prometheus.wls.rest.matchers.CommentsOnlyMatcher.containsOnlyComments;
import static io.prometheus.wls.rest.matchers.MetricsNamesSnakeCaseMatcher.usesSnakeCase;
import static io.prometheus.wls.rest.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static io.prometheus.wls.rest.matchers.ResponseHeaderMatcher.containsHeader;
import static javax.servlet.http.HttpServletResponse.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class ExporterServletTest {
    private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
    private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String TWO_VALUE_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]";
    private static final String CONFIG_WITH_CATEGORY_VALUE = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1, testSample2, bogus]";
    private static final String MULTI_QUERY_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]" +
            "\n- colors:                         \n    key: hue \n    values: wavelength";
    private WebClientFactoryStub factory = new WebClientFactoryStub();
    private ExporterServlet servlet = new ExporterServlet(factory);
    private HttpServletRequestStub request = createGetRequest();
    private HttpServletResponseStub response = createServletResponse();
    private Locale locale;

    @Before
    public void setUp() throws Exception {
        locale = Locale.getDefault();
        InMemoryFileSystem.install();
        ConfigurationUpdaterStub.install();
        LiveConfiguration.loadFromString("");
        LiveConfiguration.setServer("localhost", 7001);
        ExporterSession.cacheSession(null, null);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(locale);
        InMemoryFileSystem.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    public void exporter_isHttpServlet() throws Exception {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_hasWebServletAnnotation() throws Exception {
        assertThat(ExporterServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesMetricsPage() throws Exception {
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
    public void onGet_defineConnectionUrlFromContext() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        servlet.doGet(request, response);
        
        assertThat(factory.getClientUrl(),
                   equalTo(String.format(URL_PATTERN, LiveConfiguration.WLS_HOST, HttpServletRequestStub.PORT)));
    }

    @Test
    public void whenServerSends403StatusOnGet_returnToClient() throws Exception {
        initServlet(ONE_VALUE_CONFIG);

        factory.reportNotAuthorized();
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(SC_FORBIDDEN));
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

    private void initServlet(String configuration) throws ServletException {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, configuration);
        servlet.init(withNoParams());
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

    private Map getGroupResponseMap() {
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

    private Map getColorResponseMap() {
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

    static class WebClientFactoryStub implements WebClientFactory {
        private WebClientStub webClient = createStrictStub(WebClientStub.class);

        @Override
        public WebClient createClient() {
            return webClient;
        }

        private void addJsonResponse(Map responseMap) {
            webClient.addJsonResponse(responseMap);
        }


        private void setSetCookieResponseHeader(String setCookieHeader) {
            webClient.setCookieHeader = setCookieHeader;
        }


        private String getSentQuery() {
            return webClient.jsonQuery;
        }


        Map<String,String> getSentHeaders() {
            return webClient.sentHeaders;
        }

        private String getSentAuthentication() {
            return webClient.getAuthentication();
        }

        private String getSentSessionCookie() {
            return webClient.getSessionCookie();
        }

        private String getClientUrl() {
            return webClient.url;
        }

        private void reportNotAuthorized() {
            webClient.reportForbidden();
        }

        @SuppressWarnings("SameParameterValue")
        private void reportAuthenticationRequired(String basicRealmName) {
            webClient.reportAuthenticationRequired(basicRealmName);
        }
    }

    static abstract class WebClientStub extends WebClient {

        private String url;
        private String jsonQuery;
        private int status = SC_OK;
        private String basicRealmName;
        private String setCookieHeader;
        private List<String> responseList = new ArrayList<>();
        private Iterator<String> responses;
        private Map<String, String> addedHeaders = new HashMap<>();
        private Map<String, String> sentHeaders;

        private void addJsonResponse(Map responseMap) {
            responseList.add(new Gson().toJson(responseMap));
        }

        void reportForbidden() {
            status = SC_FORBIDDEN;
        }

        @SuppressWarnings("SameParameterValue")
        void reportAuthenticationRequired(String basicRealmName) {
            this.basicRealmName = basicRealmName;
        }

        @Override
        WebClient withUrl(String url) {
            this.url = url;
            return this;
        }

        @Override
        void addHeader(String name, String value) {
            addedHeaders.put(name, value);
        }
        @Override
        public String doPostRequest(String postBody) {
            if (url == null) throw new NullPointerException("No URL specified");
            if (status == SC_FORBIDDEN) throw new ForbiddenException();
            if (basicRealmName != null) throw new BasicAuthenticationChallengeException(basicRealmName);

            sentHeaders = Collections.unmodifiableMap(addedHeaders);
            this.jsonQuery = postBody;
            if (setCookieHeader != null)
                setSessionCookie(ExporterSession.getSessionCookie(setCookieHeader));
            return nextJsonResponse();
        }

        @Override
        public String getSetCookieHeader() {
            return setCookieHeader;
        }

        private String nextJsonResponse() {
            if (responses == null)
                responses = responseList.iterator();

            return responses.hasNext() ? responses.next() : null;
        }
    }


    abstract static class ServletInputStreamStub extends ServletInputStream {
        private InputStream inputStream;

        public ServletInputStreamStub(String contents) {
            inputStream = new ByteArrayInputStream(contents.getBytes());
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }

}
