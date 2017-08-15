package io.prometheus.wls.rest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.meterware.simplestub.Stub.createStrictStub;
import static io.prometheus.wls.rest.ExporterServlet.CONFIGURATION_FILE;
import static io.prometheus.wls.rest.ExporterServletTest.ServletConfigStub.withNoParams;
import static io.prometheus.wls.rest.ExporterServletTest.ServletConfigStub.withParams;
import static io.prometheus.wls.rest.HttpServletRequestStub.createGetRequest;
import static io.prometheus.wls.rest.HttpServletResponseStub.createServletResponse;
import static io.prometheus.wls.rest.StatusCodes.*;
import static io.prometheus.wls.rest.domain.JsonPathMatcher.hasJsonPath;
import static io.prometheus.wls.rest.matchers.CommentsOnlyMatcher.containsOnlyComments;
import static io.prometheus.wls.rest.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static io.prometheus.wls.rest.matchers.ResponseHeaderMatcher.containsHeader;
import static org.hamcrest.Matchers.*;
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
    private HttpServletRequestStub request = createGetRequest();
    private HttpServletResponseStub response = createServletResponse();

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        LiveConfiguration.loadFromString("");
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
    public void servletAnnotationIndicatesMainPage() throws Exception {
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
    public void onGet_defineConnectionUrlFromConfiguration() throws Exception {
        initServlet(String.format("---\nhost: %s\nport: %d\n", HOST, PORT));

        servlet.doGet(request, response);
        assertThat(webClient.url, equalTo(String.format(URL_PATTERN, HOST, PORT)));
    }

    @Test
    public void whenServerSends403StatusOnGet_returnToClient() throws Exception {
        initServlet("---\nqueries:\n- groups:\n    key: name\n    values: sample1");

        webClient.reportNotAuthorized();
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(NOT_AUTHORIZED));
    }

    @Test
    public void whenServerSends401StatusOnGet_returnToClient() throws Exception {
        initServlet("---\nqueries:\n- groups:\n    key: name\n    values: sample1");

        webClient.reportAuthenticationRequired("Test-Realm");
        servlet.doGet(request, response);

        assertThat(response.getStatus(), equalTo(AUTHENTICATION_REQUIRED));
        assertThat(response, containsHeader("WWW-Authenticate", "Basic realm=\"Test-Realm\""));
    }

    @Test
    public void whenClientSendsAuthenticationHeaderOnGet_passToServer() throws Exception {
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
        webClient.addJsonResponse(getGroupResponseMap());
        initServlet("---\nqueries:\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [sample1,sample2]");

        servlet.doGet(request, this.response);

        assertThat(toHtml(this.response), containsString("groupValue_sample1{name=\"first\"} 12"));
        assertThat(toHtml(this.response), containsString("groupValue_sample1{name=\"second\"} -3"));
        assertThat(toHtml(this.response), containsString("groupValue_sample2{name=\"second\"} 71.0"));
    }

    private String toHtml(HttpServletResponseStub response) {
        return response.getHtml();
    }

    private Map getGroupResponseMap() {
        return ImmutableMap.of("groups", new ItemHolder(
                    ImmutableMap.of("name", "first", "sample1", 12, "sample2", 12.3, "bogus", "red"),
                    ImmutableMap.of("name", "second", "sample1", -3, "sample2", 71.0),
                    ImmutableMap.of("name", "third", "sample1", 85, "sample2", 65.8)
        ));
    }

    // todo test: show TYPE on first instance

    @Test
    public void onGet_metricsArePrometheusCompliant() throws Exception {
        webClient.addJsonResponse(getGroupResponseMap());
        initServlet("---\nqueries:\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [sample1,sample2,bogus]");

        servlet.doGet(request, response);

        assertThat(toHtml(response), followsPrometheusRules());
    }

    @Test
    public void onGetWithMultipleQueries_displayMetrics() throws Exception {
        webClient.addJsonResponse(getGroupResponseMap());
        webClient.addJsonResponse(getColorResponseMap());
        initServlet("---\nqueries:" +
                "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [sample1,sample2]" +
                "\n- colors:                         \n    key: hue \n    values: wavelength");

        servlet.doGet(request, this.response);

        assertThat(toHtml(this.response), containsString("groupValue_sample1{name=\"first\"} 12"));
        assertThat(toHtml(this.response), containsString("groupValue_sample1{name=\"second\"} -3"));
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

    static class WebClientStub implements WebClient {
        static final String EMPTY_RESPONSE = "{}";
        private String url;
        private String username;
        private String password;
        private String jsonQuery;
        private int status = SUCCESS;
        private String basicRealmName;
        private String authenticationCredentials;
        private List<String> responseList = new ArrayList<>();
        private Iterator<String> responses;

        private void addJsonResponse(Map responseMap) {
            responseList.add(new Gson().toJson(responseMap));
        }

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
        public void defineQueryUrl(String url) {
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
            return jsonQuery.equals(ExporterServlet.EMPTY_QUERY) ? EMPTY_RESPONSE : nextJsonResponse();
        }

        private String nextJsonResponse() {
            if (responses == null)
                responses = responseList.iterator();

            return responses.hasNext() ? responses.next() : null;
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
