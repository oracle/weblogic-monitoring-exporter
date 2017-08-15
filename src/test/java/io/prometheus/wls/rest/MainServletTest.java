package io.prometheus.wls.rest;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class MainServletTest {

    private MainServlet servlet = new MainServlet();
    private HttpServletRequestStub request = HttpServletRequestStub.createGetRequest();
    private HttpServletResponseStub response = HttpServletResponseStub.createServletResponse();

    @Before
    public void setUp() throws Exception {
        LiveConfiguration.loadFromString("");
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
    public void getRequest_showsLinkToMetrics() throws Exception {
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("href=\"metrics\""));
    }

    @Test
    public void getRequest_containsConfigurationForm() throws Exception {
        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString("form action=\"configure\" method=\"post\" enctype=\"multipart/form-data\""));
    }

    @Test
    public void getRequestShowsCurrentConfiguration() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);

        servlet.doGet(request, response);

        assertThat(response.getHtml(), containsString(CONFIGURATION));
    }

    private static final String CONFIGURATION = "---\n" +
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

// assertThat(getPage(), containsForm().withAction("configure").withMethod("post").withEncType("multipart/form-data")
    // assertThat(getPage(), containsForm().withRadioButton("effect").withValues("append","replace").withDefault("append")
    //
}
