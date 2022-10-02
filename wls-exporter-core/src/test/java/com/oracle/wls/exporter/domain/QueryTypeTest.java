// Copyright (c) 2019, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.oracle.wls.exporter.domain.QueryType.CONFIGURATION;
import static com.oracle.wls.exporter.domain.QueryType.RUNTIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class QueryTypeTest implements MetricsProcessor {

    private final Map<String, Object> metrics = new HashMap<>();
    private Map<String, Object> selectedMetrics;

    @Test
    void runtimeQueryType_usesRuntimeMbeanUrl() {
        assertThat(RUNTIME.getUrlPattern(), equalTo(QueryType.RUNTIME_URL_PATTERN));
    }

    @Test
    void configurationQueryType_usesConfigMbeanUrl() {
        assertThat(CONFIGURATION.getUrlPattern(), equalTo(QueryType.CONFIGURATION_URL_PATTERN));
    }

    @Test
    void runtimeQueryType_ignoresStrings() {
        assertThat(RUNTIME.acceptsStrings(), is(false));
    }

    @Test
    void configurationQueryType_acceptsStrings() {
        assertThat(CONFIGURATION.acceptsStrings(), is(true));
    }

    @Test
    void runtimeQueryType_doesNotProcessMetrics() {
        metrics.put("name", "domain1");
        RUNTIME.postProcessMetrics(metrics, this);

        assertThat(selectedMetrics, nullValue());
    }

    @Test
    void configurationQueryType_processesNameAsDomainName() {
        metrics.put("name", "domain1");
        CONFIGURATION.postProcessMetrics(metrics, this);

        assertThat(selectedMetrics, hasEntry(QueryType.DOMAIN_KEY, "domain1"));
    }

    @Override
    public void updateConfiguration(Map<String, Object> metrics) {
        this.selectedMetrics = metrics;
    }
}