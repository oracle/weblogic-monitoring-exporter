// Copyright (c) 2019, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static com.oracle.wls.exporter.domain.QueryType.CONFIGURATION;
import static com.oracle.wls.exporter.domain.QueryType.RUNTIME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class QueryTypeTest {

    private final Map<String, Object> metrics = new HashMap<>();
    private Map<String, String> selectedMetrics;

    @Test
    public void runtimeQueryType_usesRuntimeMbeanUrl() {
        assertThat(RUNTIME.getUrlPattern(), equalTo(QueryType.RUNTIME_URL_PATTERN));
    }

    @Test
    public void configurationQueryType_usesConfigMbeanUrl() {
        assertThat(CONFIGURATION.getUrlPattern(), equalTo(QueryType.CONFIGURATION_URL_PATTERN));
    }

    @Test
    public void runtimeQueryType_ignoresStrings() {
        assertThat(RUNTIME.acceptsStrings(), is(false));
    }

    @Test
    public void configurationQueryType_acceptsStrings() {
        assertThat(CONFIGURATION.acceptsStrings(), is(true));
    }

    @Test
    public void runtimeQueryType_doesNotProcessMetrics() {
        metrics.put("name", "domain1");
        RUNTIME.processMetrics(metrics, this::invokeProcessing);

        assertThat(selectedMetrics, nullValue());
    }

    @Test
    public void configurationQueryType_processesNameAsDomainName() {
        metrics.put("name", "domain1");
        CONFIGURATION.processMetrics(metrics, this::invokeProcessing);

        assertThat(selectedMetrics, hasEntry(QueryType.DOMAIN_KEY, "domain1"));
    }

    @Test
    public void configurationQueryType_removesNameFromMetrics() {
        metrics.put("name", "domain1");
        CONFIGURATION.processMetrics(metrics, this::invokeProcessing);

        assertThat(metrics, not(hasKey("name")));
    }

    @Test
    public void whenNameNotPresent_configurationQueryTypeDoesNotInvokeProcessing() {
        CONFIGURATION.processMetrics(metrics, this::invokeProcessing);

        assertThat(selectedMetrics, nullValue());
    }

    private void invokeProcessing(Map<String, String> selectedMetrics) {
        this.selectedMetrics = selectedMetrics;
    }
}