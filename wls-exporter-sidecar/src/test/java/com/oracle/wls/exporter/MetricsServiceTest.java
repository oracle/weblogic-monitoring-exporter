package com.oracle.wls.exporter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.testsupport.TestClient;
import io.helidon.webserver.testsupport.TestResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsServiceTest {
    private static final String ONE_VALUE_CONFIG = "queries:\n- groups:\n    key: name\n    values: testSample1";

    private final WebClientFactory clientFactory = new WebClientFactoryStub();

    @BeforeEach
    void setupServer() {
        LiveConfiguration.setServer("myhost", 7123);
    }

    @Test
    void testMetricsEndpoint() throws TimeoutException, InterruptedException, ExecutionException {
        LiveConfiguration.loadFromString(ONE_VALUE_CONFIG);

        TestClient tc = TestClient
                .create(Routing.builder().register(new MetricsService(HelidonInvocationContextFactory.create(), clientFactory)));

        TestResponse testResponse = tc.path("/metrics")
                .get();

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
}