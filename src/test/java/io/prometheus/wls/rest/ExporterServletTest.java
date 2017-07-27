package io.prometheus.wls.rest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.meterware.simplestub.Stub.createStrictStub;
import static io.prometheus.wls.rest.ExporterServlet.CONFIGURATION_FILE;
import static io.prometheus.wls.rest.ExporterServletTest.ServletConfigStub.withParams;
import static io.prometheus.wls.rest.domain.JsonPathMatcher.hasJsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class ExporterServletTest {
    private static final String REST_YML = "rest.yml";
    private static final String HOST = "myhost";
    private static final int PORT = 7654;
    private static final String USER = "system";
    private static final String PASSWORD = "gumby1234";
    private List<Memento> mementos = new ArrayList<>();
    private WebClientStub webClient = new WebClientStub();
    private ExporterServlet servlet = new ExporterServlet(webClient);
    private HttpServletRequestStub request = createStrictStub(HttpServletRequestStub.class);
    private HttpServletResponseStub response = createStrictStub(HttpServletResponseStub.class);

    @Before
    public void setUp() throws Exception {
        mementos.add(InMemoryFileSystem.install());
    }

    @After
    public void tearDown() throws Exception {
        for (Memento memento : mementos) memento.revert();
    }

    @Test
    public void afterInit_servletHasAuthenticationCredentials() throws Exception {
        InMemoryFileSystem.defineFile(REST_YML, String.format("---\nusername: %s\npassword: %s\n", USER, PASSWORD));

        servlet.init(withParams(CONFIGURATION_FILE, REST_YML));

        assertThat(webClient.username, equalTo(USER));
        assertThat(webClient.password, equalTo(PASSWORD));
    }

    @Test
    public void afterInit_servletHasConnectionUrl() throws Exception {
        InMemoryFileSystem.defineFile(REST_YML, String.format("---\nhost: %s\nport: %d\n", HOST, PORT));

        servlet.init(withParams(CONFIGURATION_FILE, REST_YML));

        assertThat(webClient.url,
                   equalTo(String.format("http://%s:%d/management/weblogic/latest/serverRuntime", HOST, PORT)));
    }

    @Test
    public void onGet_sendJsonQuery() throws Exception {
        InMemoryFileSystem.defineFile(REST_YML, "---\nqueries:\n- groups:\n    key: name\n    values: sample1");
        servlet.init(withParams(CONFIGURATION_FILE, REST_YML));

        servlet.doGet(request, response);

        assertThat(webClient.jsonQuery,
                   hasJsonPath("$.children.serverRuntimes.children.groups.fields").withValues("name", "sample1"));
    }

    @Test
    public void onGet_displayMetrics() throws Exception {
        webClient.response = new Gson().toJson(getResponseMap());
        InMemoryFileSystem.defineFile(REST_YML, "---\nqueries:\n- groups:\n    prefix: groupValue_\n    key: name\n    values: sample1");
        servlet.init(withParams(CONFIGURATION_FILE, REST_YML));

        servlet.doGet(request, response);

        assertThat(toHtml(response), containsString("groupValue_sample1{name=\"first\"} red"));
        assertThat(toHtml(response), containsString("groupValue_sample1{name=\"second\"} green"));
    }

    private String toHtml(HttpServletResponseStub response) {
        return response.getHtml();
    }

    private Map getResponseMap() {
        return ImmutableMap.of("serverRuntimes",
                new ItemHolder(ImmutableMap.of("groups", new ItemHolder(
                    ImmutableMap.of("name", "first", "sample1", "red"),
                    ImmutableMap.of("name", "second", "sample1", "green"),
                    ImmutableMap.of("name", "third", "sample1", "blue")
        ))));
    }

    // todo test: no config, no queries, multiple queries
    // todo test: pass-through authentication, using the authentication from the client ?

    static class WebClientStub implements WebClient {
        private String url;
        private String username;
        private String password;
        private String jsonQuery;
        private String response = "";

        @Override
        public void initialize(String url, String username, String password) {

            this.url = url;
            this.username = username;
            this.password = password;
        }

        @Override
        public String doQuery(String jsonQuery) throws IOException {
            this.jsonQuery = jsonQuery;
            return response;
        }
    }

    static class InMemoryFileSystem implements FileSystem {
        private static Map<String, String> files;

        static Memento install() throws NoSuchFieldException {
            files = new HashMap<>();
            return StaticStubSupport.install(Files.class, "fileSystem", new InMemoryFileSystem());
        }

        static void defineFile(String filePath, String contents) {
            files.put(filePath, contents);
        }

        @Override
        public InputStream openFileFromClasspath(String fileName) throws FileNotFoundException {
            return new ByteArrayInputStream(files.get(fileName).getBytes());
        }
    }


    abstract static class ServletConfigStub implements ServletConfig {
        static ServletConfig withParams(String name1, String value1) {
            return createStrictStub(ServletConfigStub.class, ImmutableMap.of(name1, value1));
        }

        private Map<String,String> params;

        public ServletConfigStub(Map<String, String> params) {
            this.params = params;
        }

        @Override
        public String getInitParameter(String s) {
            return params.get(s);
        }
    }

    abstract static class HttpServletRequestStub implements HttpServletRequest {
        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }
    }

    abstract static class HttpServletResponseStub implements HttpServletResponse {
        private ServletOutputStreamStub out = createStrictStub(ServletOutputStreamStub.class);

        String getHtml() {
            return out.html;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return out;
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
}
