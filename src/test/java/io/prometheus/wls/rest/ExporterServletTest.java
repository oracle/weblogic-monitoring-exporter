package io.prometheus.wls.rest;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.meterware.simplestub.Stub.createStub;
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

        servlet.init(createStub(ServletConfigStub.class));

        assertThat(webClient.username, equalTo(USER));
        assertThat(webClient.password, equalTo(PASSWORD));
    }

    @Test
    public void afterInit_servletHasConnectionUrl() throws Exception {
        InMemoryFileSystem.defineFile(REST_YML, String.format("---\nhost: %s\nport: %d\n", HOST, PORT));

        servlet.init(createStub(ServletConfigStub.class));

        assertThat(webClient.url, equalTo(String.format("http://%s:%d/management/weblogic/latest/serverRuntime", HOST, PORT)));
    }

    private String getQuery() {
        return null;
    }

    static class WebClientStub implements WebClient {
        private String url;
        private String username;
        private String password;

        @Override
        public void initialize(String url, String username, String password) {

            this.url = url;
            this.username = username;
            this.password = password;
        }

        @Override
        public String doQuery(String jsonQuery) throws IOException {
            return null;
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
        @Override
        public String getInitParameter(String s) {
            return REST_YML;
        }
    }

    abstract static class HttpServletRequestStub implements HttpServletRequest {

    }

    abstract static class HttpServletResponseStub implements HttpServletResponse {

    }
}
