package io.prometheus.wls.rest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.meterware.simplestub.Stub.createStrictStub;
import static io.prometheus.wls.rest.ExporterServlet.CONFIGURATION_FILE;
import static io.prometheus.wls.rest.ExporterServletTest.ResponseHeaderMatcher.containsHeader;
import static io.prometheus.wls.rest.ExporterServletTest.ServletConfigStub.withNoParams;
import static io.prometheus.wls.rest.ExporterServletTest.ServletConfigStub.withParams;
import static io.prometheus.wls.rest.StatusCodes.*;
import static io.prometheus.wls.rest.domain.JsonPathMatcher.hasJsonPath;
import static io.prometheus.wls.rest.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class ExporterServletTest {
    private static final String REST_YML = "/rest.yml";
    private static final String HOST = "myhost";
    private static final int PORT = 7654;
    private static final String USER = "system";
    private static final String PASSWORD = "gumby1234";
    private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
    private WebClientStub webClient = new WebClientStub();
    private ExporterServlet servlet = new ExporterServlet(webClient);
    private HttpServletRequestStub request = createStrictStub(HttpServletRequestStub.class);
    private HttpServletResponseStub response = createStrictStub(HttpServletResponseStub.class);

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
    }

    @Test
    public void whenConfigParamNotFound_getReportsTheIssue() throws Exception {
        servlet.init(withNoParams());

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString(CONFIGURATION_FILE));
    }

    @Test
    public void whenConfigFileNameNotAbsolute_getReportsTheIssue() throws Exception {
        servlet.init(withParams(CONFIGURATION_FILE, "no.yml"));

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("start with"));
    }

    @Test
    public void whenConfigFileNotFound_getReportsTheIssue() throws Exception {
        servlet.init(withParams(CONFIGURATION_FILE, REST_YML));

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString(REST_YML));
    }

    @Test
    public void afterInit_servletHasAuthenticationCredentials() throws Exception {
        initServlet(String.format("---\nusername: %s\npassword: %s\n", USER, PASSWORD));

        assertThat(webClient.username, equalTo(USER));
        assertThat(webClient.password, equalTo(PASSWORD));
    }

    @Test
    public void afterInit_servletHasConnectionUrl() throws Exception {
        initServlet(String.format("---\nhost: %s\nport: %d\n", HOST, PORT));

        assertThat(webClient.url, equalTo(String.format(URL_PATTERN, HOST, PORT)));
    }

    @Test
    public void whenServerSends403Status_returnToClient() throws Exception {
        initServlet("---\nqueries:\n- groups:\n    key: name\n    values: sample1");

        webClient.reportNotAuthorized();
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(NOT_AUTHORIZED));
    }

    @Test
    public void whenServerSends401Status_returnToClient() throws Exception {
        initServlet("---\nqueries:\n- groups:\n    key: name\n    values: sample1");

        webClient.reportAuthenticationRequired("Test-Realm");
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(AUTHENTICATION_REQUIRED));
        assertThat(response, containsHeader("WWW-Authenticate", "Basic realm=\"Test-Realm\""));
    }

    @Test
    public void whenClientSendsAuthenticationHeader_passToServer() throws Exception {
        initServlet("---\nqueries:\n- groups:\n    key: name\n    values: sample1");

        request.setHeader("Authorization", "auth-credentials");
        servlet.doGet(request, response);

        assertThat(webClient.getAuthenticationCredentials(), equalTo("auth-credentials"));
    }

    @Test
    public void onGet_sendJsonQuery() throws Exception {
        initServlet("---\nqueries:\n- groups:\n    key: name\n    values: sample1");

        servlet.doGet(request, response);

        assertThat(webClient.jsonQuery,
                   hasJsonPath("$.children.groups.fields").withValues("name", "sample1"));
    }

    private void initServlet(String configuration) throws ServletException {
        InMemoryFileSystem.defineResource(REST_YML, configuration);
        servlet.init(withParams(CONFIGURATION_FILE, REST_YML));
    }

    @Test
    public void onGet_displayMetrics() throws Exception {
        webClient.response = new Gson().toJson(getResponseMap());
        initServlet("---\nqueries:\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [sample1,sample2]");

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("groupValue_sample1{name=\"first\"} 12"));
        assertThat(toHtml(response), containsString("groupValue_sample1{name=\"second\"} -3"));
        assertThat(toHtml(response), containsString("groupValue_sample2{name=\"second\"} 71.0"));
    }

    private String toHtml(HttpServletResponseStub response) {
        return response.getHtml();
    }

    private Map getResponseMap() {
        return ImmutableMap.of("groups", new ItemHolder(
                    ImmutableMap.of("name", "first", "sample1", 12, "sample2", 12.3, "bogus", "red"),
                    ImmutableMap.of("name", "second", "sample1", -3, "sample2", 71.0),
                    ImmutableMap.of("name", "third", "sample1", 85, "sample2", 65.8)
        ));
    }

    // todo test: no config, no queries, multiple queries
    // todo test: show TYPE on first instance

    @Test
    public void onGet_metricsArePrometheusCompliant() throws Exception {
        webClient.response = new Gson().toJson(getResponseMap());
        initServlet("---\nqueries:\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [sample1,sample2,bogus]");

        servlet.doGet(request, response);

        assertThat(toHtml(response), followsPrometheusRules());
    }

    static class WebClientStub implements WebClient {
        private String url;
        private String username;
        private String password;
        private String jsonQuery;
        private String response = "";
        private int status = SUCCESS;
        private String basicRealmName;
        private String authenticationCredentials;

        void reportNotAuthorized() {
            status = NOT_AUTHORIZED;
        }

        @SuppressWarnings("SameParameterValue")
        void reportAuthenticationRequired(String basicRealmName) {
            this.basicRealmName = basicRealmName;
        }

        String getAuthenticationCredentials() {
            return authenticationCredentials;
        }

        @Override
        public void initialize(String url) {
            this.url = url;
        }

        @Override
        public void setCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void setAuthenticationCredentials(String authenticationCredentials) {
            this.authenticationCredentials = authenticationCredentials;
        }

        @Override
        public String doQuery(String jsonQuery) throws IOException {
            if (status == NOT_AUTHORIZED) throw new NotAuthorizedException();
            if (basicRealmName != null) throw new BasicAuthenticationChallengeException(basicRealmName);
            
            this.jsonQuery = jsonQuery;
            return response;
        }
    }

    static class InMemoryFileSystem {
        private static Map<String, InputStream> resources;

        static void install() throws NoSuchFieldException {
            resources = new HashMap<>();
        }

        static void defineResource(String filePath, String contents) {
            resources.put(filePath, toInputStream(contents));
        }

        private static InputStream toInputStream(String contents) {
            return new ByteArrayInputStream(contents.getBytes());
        }
    }


    abstract static class ServletConfigStub implements ServletConfig {
        static ServletConfig withNoParams() {
            return createStrictStub(ServletConfigStub.class, ImmutableMap.of());
        }

        static ServletConfig withParams(String name1, String value1) {
            return createStrictStub(ServletConfigStub.class, ImmutableMap.of(name1, value1));
        }

        private Map<String,String> params;
        private ServletContext context;

        public ServletConfigStub(Map<String, String> params) {
            this.params = params;
            context = createStrictStub(ServletContextStub.class);
        }

        @Override
        public String getInitParameter(String s) {
            return params.get(s);
        }

        @Override
        public ServletContext getServletContext() {
            return context;
        }
    }

    abstract static class ServletContextStub implements ServletContext {

        @Override
        public InputStream getResourceAsStream(String path) {
            return !REST_YML.equals(path) ?  null : InMemoryFileSystem.resources.get(path);
        }
    }

    abstract static class HttpServletRequestStub implements HttpServletRequest {
        private Map<String,String> headers = new HashMap<>();

        @SuppressWarnings("SameParameterValue")
        void setHeader(String headerName, String headerValue) {
            headers.put(headerName, headerValue);
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }
    }

    abstract static class HttpServletResponseStub implements HttpServletResponse {
        private int status = SUCCESS;
        private ServletOutputStreamStub out = createStrictStub(ServletOutputStreamStub.class);
        private Map<String,List<String>> headers = new HashMap<>();
        private boolean responseSent = false;

        String getHtml() {
            return out.html;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return out;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            status = sc;
            responseSent = true;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public void setHeader(String name, String value) {
            assert !responseSent : "May not set headers after response has been sent";
            headers.put(name, Collections.singletonList(value));
        }

        @Override
        public boolean containsHeader(String name) {
            return headers.containsKey(name);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return headers.keySet();
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return headers.containsKey(name) ? headers.get(name) : Collections.emptyList();
        }
    }

    abstract static class ServletOutputStreamStub extends ServletOutputStream {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private String html;

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            html = baos.toString("UTF-8");
        }
    }

    static class ResponseHeaderMatcher extends TypeSafeDiagnosingMatcher<HttpServletResponse> {
        private String expectedHeaderName;
        private String expectedHeaderValue;

        private ResponseHeaderMatcher(String expectedHeaderName, String expectedHeaderValue) {
            this.expectedHeaderName = expectedHeaderName;
            this.expectedHeaderValue = expectedHeaderValue;
        }

        static ResponseHeaderMatcher containsHeader(String expectedHeaderName, String expectedHeaderValue) {
            return new ResponseHeaderMatcher(expectedHeaderName, expectedHeaderValue);
        }

        @Override
        protected boolean matchesSafely(HttpServletResponse response, Description description) {
            if (!response.containsHeader(expectedHeaderName))
                return reportNoSuchHeader(response, description);
            else if (!response.getHeaders(expectedHeaderName).contains(expectedHeaderValue))
                return reportNoMatchingHeaderValue(response, description);
            else
                return true;
        }

        private boolean reportNoSuchHeader(HttpServletResponse response, Description description) {
            description.appendValueList("found header names: {", ", ", "}", response.getHeaderNames());
            return false;
        }

        private boolean reportNoMatchingHeaderValue(HttpServletResponse response, Description description) {
            description.appendText("found ").appendText(expectedHeaderName)
                    .appendValueList(" with value(s) ", ", ", "", response.getHeaders(expectedHeaderName));
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("response containing header ")
                    .appendValue(expectedHeaderName)
                    .appendText(" with value ")
                    .appendValue(expectedHeaderValue);
        }
    }

}
