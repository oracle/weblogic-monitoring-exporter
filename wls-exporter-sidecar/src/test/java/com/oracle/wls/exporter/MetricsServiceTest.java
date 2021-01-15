// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.oracle.wls.exporter.domain.ExporterConfig;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.Routing;
import io.helidon.webserver.testsupport.MediaPublisher;
import io.helidon.webserver.testsupport.TestClient;
import io.helidon.webserver.testsupport.TestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.matchers.JsonPathMatcher.hasJsonPath;
import static com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsServiceTest {

    private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String TWO_VALUE_CONFIG = "queries:" +
            "\n- groups:\n    prefix: groupValue_\n    key: name\n    values: [testSample1,testSample2]";
    private static final String NO_CONFIGURATION = "";

    private final WebClientFactoryStub clientFactory = new WebClientFactoryStub();
    private TestClient client;
    private final List<Memento> mementos = new ArrayList<>();

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        mementos.add(StaticStubSupport.install(ExporterConfig.class, "defaultSnakeCaseSetting", false));
        LiveConfiguration.setServer("myhost", 7123);
        LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);
        client = TestClient.create(Routing.builder().register(createMetricsService()));
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    private MetricsService createMetricsService() {
        return new MetricsService(new SidecarConfiguration(), clientFactory);
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

    // todo test WLS host/port info

    @Test
    public void whenServerSends403StatusOnGet_returnToClient() throws Exception {
        clientFactory.reportNotAuthorized();

        assertThat(getMetricsResponse().status().code(), equalTo(HTTP_FORBIDDEN));
    }

    @Test
    public void whenServerSends400StatusOnGet_reportErrorInComments() throws Exception {
        clientFactory.reportBadQuery();

        assertThat(getMetrics(), followsPrometheusRules());
    }

    @Test
    public void whenServerSends401StatusOnGet_returnToClient() throws Exception {
        clientFactory.reportAuthenticationRequired("Test-Realm");
        final TestResponse metricsResponse = getMetricsResponse();

        assertThat(metricsResponse.status().code(), equalTo(HTTP_UNAUTHORIZED));
        assertThat(metricsResponse.headers().first("WWW-Authenticate").orElse(null), equalTo("Basic realm=\"Test-Realm\""));
    }

    @Test
    public void whenClientSendsAuthenticationHeader_passToServer() throws Exception {
        client.path("/metrics").header(AUTHENTICATION_HEADER, "auth-credentials").get();

        assertThat(clientFactory.getSentAuthentication(), equalTo("auth-credentials"));
    }

    @Test
    public void onGet_sendJsonQuery() throws Exception {
        getMetricsResponse();

        assertThat(clientFactory.getSentQuery(),
                   hasJsonPath("$.children.groups.fields").withValues("name", "testSample1"));
    }

    @Test
    void onGet_sendXRequestedHeader() throws TimeoutException, InterruptedException {
        getMetricsResponse();

        assertThat(clientFactory.getSentHeaders(), hasKey("X-Requested-By"));
    }

    @Test
    public void onGet_displayMetrics() throws Exception {
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
}