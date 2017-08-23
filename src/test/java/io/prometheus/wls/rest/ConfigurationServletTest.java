package io.prometheus.wls.rest;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static io.prometheus.wls.rest.HttpServletRequestStub.createPostRequest;
import static io.prometheus.wls.rest.HttpServletResponseStub.createServletResponse;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class ConfigurationServletTest {

    private ConfigurationServlet servlet = new ConfigurationServlet();
    private HttpServletResponseStub response = createServletResponse();

    @Before
    public void setUp() throws Exception {
        LiveConfiguration.loadFromString("");
    }

    @Test
    public void configuration_isHttpServlet() throws Exception {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_hasWebServletAnnotation() throws Exception {
        assertThat(ConfigurationServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesConfigurationPage() throws Exception {
        WebServlet annotation = ConfigurationServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/configure"));
    }

    private final static String BOUNDARY = "C3n5NKoslNBKj4wBHR8kCX6OtVYEqeFYNjorlBP";
    private static final String CONFIGURATION = "---\n" +
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String ADDED_CONFIGURATION = "---\n" +
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    private static final String COMBINED_CONFIGURATION = "---\n" +
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";


    @Test(expected = ServletException.class)
    public void whenPostWithoutFile_reportFailure() throws Exception {
        servlet.doPost(createPostRequest(), response);
    }

    @Test
    public void afterUploadWithReplace_useNewConfiguration() throws Exception {
        servlet.doPost(createUploadRequest(createEncodedForm("replace", CONFIGURATION)), response);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    public void afterUpload_redirectToMainPage() throws Exception {
        servlet.doPost(createUploadRequest(createEncodedForm("replace", CONFIGURATION)), response);

        assertThat(response.getRedirectLocation(), equalTo(""));
    }

    private HttpServletRequestStub createUploadRequest(String contents) {
        HttpServletRequestStub postRequest = createPostRequest();
        postRequest.setMultipartContent(contents, BOUNDARY);
        return postRequest;
    }

    private String createEncodedForm(String effect, String configuration) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setBoundary(BOUNDARY);
        builder.addTextBody("effect", effect);
        builder.addBinaryBody("configuration", configuration.getBytes(), ContentType.create("text/plain", Charset.defaultCharset()), "newconfig.yml");
        HttpEntity entity = builder.build();
        return asString(entity);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String asString(HttpEntity entity) throws IOException {
        byte[] result = new byte[(int) entity.getContentLength()];
        InputStream inputStream = entity.getContent();
        inputStream.read(result);
        return new String(result);
    }


    @Test
    public void afterUploadWithAppend_useBothConfiguration() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", ADDED_CONFIGURATION)), createServletResponse());

        assertThat(LiveConfiguration.asString(), equalTo(COMBINED_CONFIGURATION));
    }

    @Test (expected = ServletException.class)
    public void whenSelectedFileIsNotYaml_reportError() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", NON_YAML)), createServletResponse());

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    private static final String NON_YAML = "---\n" +
            "this is not yaml\n";

    @Test (expected = ServletException.class)
    public void whenSelectedFileHasBadBooleanValue_reportError() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", ADDED_CONFIGURATION_WITH_BAD_BOOLEAN)), createServletResponse());
    }

    private static final String ADDED_CONFIGURATION_WITH_BAD_BOOLEAN = "---\n" +
            "host: localhost\n" +
            "port: 7001\n" +
            "metricsNameSnakeCase: blabla\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";
}
