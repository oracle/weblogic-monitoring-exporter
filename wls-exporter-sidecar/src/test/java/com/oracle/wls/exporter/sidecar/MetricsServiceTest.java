// Copyright (c) 2021, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.LiveConfiguration;
import com.oracle.wls.exporter.domain.ExporterConfig;
import io.helidon.http.Http;
import io.helidon.common.media.type.MediaType;
import io.helidon.webserver.Routing;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.testsupport.MediaPublisher;
import io.helidon.webserver.testsupport.TestClient;
import io.helidon.webserver.testsupport.TestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.meterware.simplestub.Stub.createStub;
import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_CHALLENGE_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.domain.QueryType.RUNTIME_URL_PATTERN;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsServiceTest {

    private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String TWO_VALUE_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]";
    private static final String NO_CONFIGURATION = "";

    private static final String JSON_TWO_VALUE_CONFIG = "{\n" +
          "  \"metricsNameSnakeCase\": true,\n" +
          "  \"queries\": [\n" +
          "    {\n" +
          "      \"groups\": {\n" +
          "        \"key\": \"name\",\n" +
          "        \"prefix\": \"group_value_\",\n" +
          "        \"values\": [\n" +
          "           \"testSample1\",\n" +
          "           \"testSample2\"\n" +
          "          ]\n" +
          "      }\n" +
          "    }\n" +
          "  ]\n" +
          "}";

    private final WebClientFactoryStub clientFactory = new WebClientFactoryStub();
    private TestClient client;
    private final List<Memento> mementos = new ArrayList<>();

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        mementos.add(StaticStubSupport.install(ExporterConfig.class, "defaultSnakeCaseSetting", true));
        SidecarConfigurationTestSupport.preserveConfigurationProperties(mementos);
        LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);
        client = TestClient.create(HttpRouting.builder().register(createMetricsService()));
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    private MetricsService createMetricsService() {
        return new MetricsService(new SidecarConfiguration(), clientFactory);
    }

    //------------- sidecar configuration ------

    @Test
    void usePropertiesToConfigureListeningPort() {
        System.setProperty(SidecarConfiguration.LISTEN_PORT_PROPERTY, "8000");

        assertThat(createMetricsService().getListenPort(), equalTo(8000));
    }

    @Test
    void usePropertiesToConfigurePlaintextWLSUrl() {
        System.setProperty(SidecarConfiguration.WLS_HOST_PROPERTY, "plainHost");
        System.setProperty(SidecarConfiguration.WLS_PORT_PROPERTY, "7111");

        assertThat(createHelidonInvocationContext().createUrlBuilder().createUrl(RUNTIME_URL_PATTERN),
              startsWith("http://plainHost:7111"));
    }

    private InvocationContext createHelidonInvocationContext() {
        return new HelidonInvocationContext(createStub(ServerRequest.class), createStub(ServerResponse.class));
    }

    @Test
    void usePropertiesToConfigureSecureWLSUrl() {
        System.setProperty(SidecarConfiguration.WLS_HOST_PROPERTY, "secureHost");
        System.setProperty(SidecarConfiguration.WLS_PORT_PROPERTY, "7333");
        System.setProperty(SidecarConfiguration.WLS_SECURE_PROPERTY, "true");

        assertThat(createHelidonInvocationContext().createUrlBuilder().createUrl(RUNTIME_URL_PATTERN),
              startsWith("https://secureHost:7333"));
    }
    
//------------- metrics --------

    @Test
    void whenNoConfiguration_reportTheIssue() throws TimeoutException, InterruptedException, ExecutionException {
        LiveConfiguration.loadFromString(NO_CONFIGURATION);

        String response = getMetrics();

        assertThat(response, containsString("# No configuration"));
    }

    private String getMetrics() throws InterruptedException, ExecutionException, TimeoutException {
        return getMetricsResponse().asString().get();
    }

    private TestResponse getMetricsResponse() throws InterruptedException, TimeoutException {
        return client.path("/metrics").get();
    }

    // todo test WLS hostName/port info

    @Test
    void whenServerSends403StatusOnGet_returnToClient() throws Exception {
        clientFactory.reportNotAuthorized();

        assertThat(getMetricsResponse().status().code(), equalTo(HTTP_FORBIDDEN));
    }

    @Test
    void whenServerSends401StatusOnGet_returnToClient() throws Exception {
        clientFactory.reportAuthenticationRequired("Test-Realm");
        final TestResponse metricsResponse = getMetricsResponse();

        assertThat(metricsResponse.status().code(), equalTo(HTTP_UNAUTHORIZED));
        assertThat(getAuthenticationChallengeHeader(metricsResponse), equalTo("Basic realm=\"Test-Realm\""));
    }

    private String getAuthenticationChallengeHeader(TestResponse metricsResponse) {
        return metricsResponse.headers().first(AUTHENTICATION_CHALLENGE_HEADER).orElse(null);
    }

    @Test
    void whenClientSendsAuthenticationHeader_passToServer() throws Exception {
        client.path("/metrics").header(AUTHENTICATION_HEADER, "auth-credentials").get();

        assertThat(clientFactory.getSentAuthentication(), equalTo("auth-credentials"));
    }

    @Test
    void onGet_sendXRequestedHeader() throws TimeoutException, InterruptedException {
        getMetricsResponse();

        assertThat(clientFactory.getSentHeaders(), hasKey("X-Requested-By"));
    }

    @Test
    void onGet_displayMetrics() throws Exception {
        LiveConfiguration.loadFromString(TWO_VALUE_CONFIG);
        clientFactory.addJsonResponse(getGroupResponseMap());

        final String metrics = getMetrics();

        assertThat(metrics, containsString("group_value_test_sample1{name=\"first\"} 12"));
        assertThat(metrics, containsString("group_value_test_sample1{name=\"second\"} -3"));
        assertThat(metrics, containsString("group_value_test_sample2{name=\"second\"} 71.0"));
    }

    private Map<String,Object> getGroupResponseMap() {
        return Map.of("groups", new ItemHolder(
                    Map.of("name", "first", "testSample1", 12, "testSample2", 12.3, "bogus", "red"),
                    Map.of("name", "second", "testSample1", -3, "testSample2", 71.0),
                    Map.of("name", "third", "testSample1", 85, "testSample2", 65.8)
        ));
    }

    @Test
    void testMetricsEndpoint() throws TimeoutException, InterruptedException, ExecutionException {
        TestResponse testResponse = getMetricsResponse();

        assertEquals(Http.Status.OK_200, testResponse.status());

        String response = testResponse.asString().get();

        validateResponse(response, "wls_scrape_mbeans_count_total{instance=\"");
        validateResponse(response, "wls_scrape_duration_seconds{instance=\"");
        validateResponse(response, "wls_scrape_cpu_seconds{instance=\"");
    }

    void validateResponse(String response, String toCheck) {
        assertTrue(response.contains(toCheck),
                   "Response should contain " + toCheck + ", but is: " + response);
    }


    // -------------- put configuration  ---------

    @Test
    void afterPutConfiguration_displayMetrics() throws TimeoutException, InterruptedException, ExecutionException {
        client.path("/configuration").put(MediaPublisher.create(MediaType.APPLICATION_YAML, TWO_VALUE_CONFIG));
        clientFactory.addJsonResponse(getGroupResponseMap());

        final String metrics = getMetrics();

        assertThat(metrics, containsString("group_value_test_sample1{name=\"first\"} 12"));
        assertThat(metrics, containsString("group_value_test_sample1{name=\"second\"} -3"));
        assertThat(metrics, containsString("group_value_test_sample2{name=\"second\"} 71.0"));
    }

    @Test
    void afterPutJsonConfiguration_displayMetrics() throws TimeoutException, InterruptedException, ExecutionException {
        client.path("/configuration").put(MediaPublisher.create(MediaType.APPLICATION_YAML, JSON_TWO_VALUE_CONFIG));
        clientFactory.addJsonResponse(getGroupResponseMap());

        final String metrics = getMetrics();

        assertThat(metrics, containsString("group_value_test_sample1{name=\"first\"} 12"));
        assertThat(metrics, containsString("group_value_test_sample1{name=\"second\"} -3"));
        assertThat(metrics, containsString("group_value_test_sample2{name=\"second\"} 71.0"));
    }

    // ---------- get configuration --------

    @Test
    void retrieveConfiguration() throws TimeoutException, InterruptedException, ExecutionException {
        final TestResponse testResponse = client.path("/").get();

        assertEquals(Http.Status.OK_200, testResponse.status());

        String response = testResponse.asString().get();

        assertThat(response, containsString(ONE_VALUE_CONFIG));
    }

    // -------------- get interactions ----------


    @Test
    void afterMetricsReceived_viewMessages() throws TimeoutException, InterruptedException, ExecutionException {
        client.path("/configuration").put(MediaPublisher.create(MediaType.APPLICATION_YAML, JSON_TWO_VALUE_CONFIG));
        clientFactory.addJsonResponse(getGroupResponseMap());
        getMetrics();

        final TestResponse testResponse = client.path("/messages").get();

        assertEquals(Http.Status.OK_200, testResponse.status());

        String response = testResponse.asString().get();

        assertThat(response, stringContainsInOrder(Arrays.asList("REQUEST to", "fields", "testSample1")));
    }


}