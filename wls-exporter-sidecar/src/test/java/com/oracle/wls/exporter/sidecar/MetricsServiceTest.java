// Copyright (c) 2021, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.LiveConfiguration;
import com.oracle.wls.exporter.domain.ExporterConfig;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Header;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.meterware.simplestub.Stub.createStub;
import static com.oracle.wls.exporter.domain.QueryType.RUNTIME_URL_PATTERN;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RoutingTest
class MetricsServiceTest {

    private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";
    private static final String TWO_VALUE_CONFIG = """
            queries:\
            
            - groups:
                prefix: groupValue_
                key: name
                values: [testSample1,testSample2]""";
    private static final String NO_CONFIGURATION = "";

    private static final String JSON_TWO_VALUE_CONFIG = """
            {
              "metricsNameSnakeCase": true,
              "queries": [
                {
                  "groups": {
                    "key": "name",
                    "prefix": "group_value_",
                    "values": [
                       "testSample1",
                       "testSample2"
                      ]
                  }
                }
              ]
            }""";

    private static final WebClientFactoryStub clientFactory = new WebClientFactoryStub();
    private final List<Memento> mementos = new ArrayList<>();
    private final WebClient client;

    MetricsServiceTest(WebClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUpRoute(HttpRouting.Builder routing) {
        routing.register(createMetricsService());
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        mementos.add(StaticStubSupport.install(ExporterConfig.class, "defaultSnakeCaseSetting", true));
        SidecarConfigurationTestSupport.preserveConfigurationProperties(mementos);
        LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    private static MetricsService createMetricsService() {
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
    void whenNoConfiguration_reportTheIssue() {
        LiveConfiguration.loadFromString(NO_CONFIGURATION);

        String response = getMetrics();

        assertThat(response, containsString("# No configuration"));
    }

    private String getMetrics() {
        return getMetricsResponse().as(String.class);
    }

    private HttpClientResponse getMetricsResponse() {
        return client.get("/metrics").request();
    }

    @Test
    void whenServerSends403StatusOnGet_returnToClient() {
        clientFactory.reportNotAuthorized();

        assertThat(getMetricsResponse().status().code(), equalTo(HTTP_FORBIDDEN));
    }

    @Test
    void whenServerSends401StatusOnGet_returnToClient() {
        clientFactory.reportAuthenticationRequired("Test-Realm");
        final HttpClientResponse metricsResponse = getMetricsResponse();

        assertThat(metricsResponse.status().code(), equalTo(HTTP_UNAUTHORIZED));
        assertThat(getAuthenticationChallengeHeader(metricsResponse), equalTo("Basic realm=\"Test-Realm\""));
    }

    private String getAuthenticationChallengeHeader(HttpClientResponse metricsResponse) {
        return metricsResponse.headers().first(HeaderNames.WWW_AUTHENTICATE).orElse(null);
    }

    @Test
    void whenClientSendsAuthenticationHeader_passToServer() {
        client.get("/metrics")
                .header(HeaderNames.AUTHORIZATION, "auth-credentials")
                .request();

        assertThat(clientFactory.getSentAuthentication(), equalTo("auth-credentials"));
    }

    @Test
    void onGet_sendXRequestedHeader() {
        getMetricsResponse();

        assertThat(clientFactory.getSentHeaders(), hasKey("X-Requested-By"));
    }

    @Test
    void metricsResponseContainsContentTypeHeader() {
      Header contentTypeHeader = getContentTypeHeader(getMetricsResponse());

        assertThat(contentTypeHeader, notNullValue());
        assertThat(contentTypeHeader.values(), equalTo("text/plain"));
    }

    private Header getContentTypeHeader(HttpClientResponse metricsResponse) {
        return metricsResponse.headers().stream().filter(this::isContentType).findFirst().orElse(null);
    }

    private boolean isContentType(Header header) {
        return "Content-Type".equalsIgnoreCase(header.name());
    }

    @Test
    void onGet_displayMetrics() {
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
    void testMetricsEndpoint() {
        HttpClientResponse testResponse = getMetricsResponse();

        assertEquals(Status.OK_200, testResponse.status());

        String response = testResponse.as(String.class);

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
    void afterPutConfiguration_displayMetrics() {
        client.put("/configuration")
                .contentType(MediaTypes.APPLICATION_YAML)
                .submit(TWO_VALUE_CONFIG);
        clientFactory.addJsonResponse(getGroupResponseMap());

        final String metrics = getMetrics();

        assertThat(metrics, containsString("group_value_test_sample1{name=\"first\"} 12"));
        assertThat(metrics, containsString("group_value_test_sample1{name=\"second\"} -3"));
        assertThat(metrics, containsString("group_value_test_sample2{name=\"second\"} 71.0"));
    }

    @Test
    void afterPutJsonConfiguration_displayMetrics() {
        client.put("/configuration")
                .contentType(MediaTypes.APPLICATION_YAML)
                .submit(JSON_TWO_VALUE_CONFIG);
        clientFactory.addJsonResponse(getGroupResponseMap());

        final String metrics = getMetrics();

        assertThat(metrics, containsString("group_value_test_sample1{name=\"first\"} 12"));
        assertThat(metrics, containsString("group_value_test_sample1{name=\"second\"} -3"));
        assertThat(metrics, containsString("group_value_test_sample2{name=\"second\"} 71.0"));
    }

    // ---------- get configuration --------

    @Test
    void retrieveConfiguration() {
        final ClientResponseTyped<String> testResponse = client.get("/").request(String.class);

        assertEquals(Status.OK_200, testResponse.status());

        String response = testResponse.entity();

        assertThat(response, containsString(ONE_VALUE_CONFIG));
    }

    // -------------- get interactions ----------


    @Test
    void afterMetricsReceived_viewMessages() {
        client.put("/configuration")
                .contentType(MediaTypes.APPLICATION_YAML)
                .submit(JSON_TWO_VALUE_CONFIG);
        clientFactory.addJsonResponse(getGroupResponseMap());
        getMetrics();

        final ClientResponseTyped<String> testResponse = client.get("/messages").request(String.class);

        assertEquals(Status.OK_200, testResponse.status());

        String response = testResponse.entity();

        assertThat(response, stringContainsInOrder(Arrays.asList("REQUEST to", "fields", "testSample1")));
    }


}