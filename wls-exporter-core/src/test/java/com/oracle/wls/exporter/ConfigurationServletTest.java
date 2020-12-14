// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.oracle.wls.exporter.domain.ConfigurationException;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.wls.exporter.HttpServletRequestStub.LOCAL_PORT;
import static com.oracle.wls.exporter.HttpServletRequestStub.createPostRequest;
import static com.oracle.wls.exporter.HttpServletResponseStub.createServletResponse;
import static com.oracle.wls.exporter.MultipartTestUtils.createEncodedForm;
import static com.oracle.wls.exporter.MultipartTestUtils.createUploadRequest;
import static com.oracle.wls.exporter.ServletConstants.CONFIGURATION_PAGE;
import static com.oracle.wls.exporter.matchers.ResponseHeaderMatcher.containsHeader;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class ConfigurationServletTest {

    private final static int REST_PORT = 7651;

    private final WebClientFactoryStub factory = new WebClientFactoryStub();
    private final ConfigurationServlet servlet = new ConfigurationServlet(factory);
    private final HttpServletResponseStub response = createServletResponse();
    private HttpServletRequestStub request;

    @Before
    public void setUp() throws Exception {
        LiveConfiguration.loadFromString("");
        request = createUploadRequest(createEncodedForm("replace", CONFIGURATION));
        UrlBuilder.clearHistory();
    }

    @Test
    public void configuration_isHttpServlet() {
        assertThat(servlet, instanceOf(HttpServlet.class));
    }

    @Test
    public void servlet_hasWebServletAnnotation() {
        assertThat(ConfigurationServlet.class.getAnnotation(WebServlet.class), notNullValue());
    }

    @Test
    public void servletAnnotationIndicatesConfigurationPage() {
        WebServlet annotation = ConfigurationServlet.class.getAnnotation(WebServlet.class);

        assertThat(annotation.value(), arrayContaining("/" + CONFIGURATION_PAGE));
    }

    private static final String CONFIGURATION =
            "host: " + HttpServletRequestStub.HOST + "\n" +
            "port: " + HttpServletRequestStub.PORT + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String CONFIGURATION_WITH_REST_PORT =
            "host: " + HttpServletRequestStub.HOST + "\n" +
            "port: " + HttpServletRequestStub.PORT + "\n" +
            "restPort: " + REST_PORT + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String ADDED_CONFIGURATION =
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    private static final String COMBINED_CONFIGURATION =
            "host: " + HttpServletRequestStub.HOST + "\n" +
            "port: " + HttpServletRequestStub.PORT + "\n" +
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
    public void whenRequestUsesHttp_authenticateWithHttp() throws Exception {
        servlet.doPost(createUploadRequest(createEncodedForm("replace", CONFIGURATION)), response);

        assertThat(factory.getClientUrl(), Matchers.startsWith("http:"));
    }

    @Test
    public void whenRequestUsesHttps_authenticateWithHttps() throws Exception {
        HttpServletRequestStub request = createUploadRequest(createEncodedForm("replace", CONFIGURATION));
        request.setSecure(true);
        servlet.doPost(request, response);

        assertThat(factory.getClientUrl(), Matchers.startsWith("https:"));
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

    @Test
    public void whenRestPortInaccessible_switchToLocalPort() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION_WITH_REST_PORT);
        factory.throwConnectionFailure("localhost", REST_PORT);

        servlet.doPost(createUploadRequest(createEncodedForm("replace", CONFIGURATION_WITH_REST_PORT)), response);

        assertThat(createAuthenticationUrl(), containsString(Integer.toString(LOCAL_PORT)));
    }

    private String createAuthenticationUrl() {
        servlet.createWebClient(HttpServletRequestStub.createGetRequest());
        return servlet.getAuthenticationUrl();
    }

    @Test
    public void afterUploadWithAppend_useBothConfiguration() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", ADDED_CONFIGURATION)), createServletResponse());

        assertThat(LiveConfiguration.asString(), equalTo(COMBINED_CONFIGURATION));
    }

    @Test
    public void whenSelectedFileIsNotYaml_reportError() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", NON_YAML)), response);

        assertThat(response.getHtml(), containsString(ConfigurationException.NOT_YAML_FORMAT));
    }

    private static final String NON_YAML =
            "this is not yaml\n";

    @Test
    public void whenSelectedFileHasPartialYaml_reportError() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", PARTIAL_YAML)), response);

        assertThat(response.getHtml(), containsString(ConfigurationException.BAD_YAML_FORMAT));
    }

    private static final String PARTIAL_YAML =
            "queries:\nkey name\n";

    @Test
    public void whenSelectedFileHasBadBooleanValue_reportError() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", ADDED_CONFIGURATION_WITH_BAD_BOOLEAN)), response);

        assertThat(response.getHtml(), containsString("blabla"));
    }

    @Test
    public void afterSelectedFileHasBadBooleanValue_configurationIsUnchanged() throws Exception {
        LiveConfiguration.loadFromString(CONFIGURATION);
        servlet.doPost(createUploadRequest(createEncodedForm("append", ADDED_CONFIGURATION_WITH_BAD_BOOLEAN)), response);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    private static final String ADDED_CONFIGURATION_WITH_BAD_BOOLEAN =
            "host: localhost\n" +
            "port: 7001\n" +
            "metricsNameSnakeCase: blabla\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    @Test
    public void whenServerSends403StatusOnGet_returnToClient() throws Exception {
        factory.reportNotAuthorized();
        servlet.doPost(request, response);

        assertThat(response.getStatus(), equalTo(SC_FORBIDDEN));
    }

    @Test
    public void whenServerSends401StatusOnGet_returnToClient() throws Exception {
        factory.reportAuthenticationRequired("Test-Realm");
        servlet.doPost(request, response);

        assertThat(response.getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(response, containsHeader("WWW-Authenticate", "Basic realm=\"Test-Realm\""));
    }
}
