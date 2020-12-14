// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.matchers;

import java.util.Arrays;

import org.junit.Test;

import static com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Russell Gold
 */
public class PrometheusMetricsMatcherTest {

    private final PrometheusMetricsMatcher matcher = followsPrometheusRules();

    @Test
    public void whenMetricsAreGrouped_matcherPasses() {
        assertTrue(matcher.matches(toHtml(ORDERED_LIST)));
    }

    private final String[] ORDERED_LIST =
            {"metric1 1", "metric2{name='red'} 23", "metric2{name='blue'} 34"};

    private String toHtml(String... metrics) {
        return Arrays.stream(metrics).map((s) -> s.replace('\'', '"')).collect(joining("\n"));
    }

    @Test
    public void whenMetricsAreInterspersed_matcherFails() {
        assertFalse(matcher.matches(toHtml(MISORDERED_LIST)));
    }

    private final String[] MISORDERED_LIST =
            {"metric2{name='red'} 23", "metric1 1", "metric2{name='blue'} 34"};

    @Test
    public void whenMetricsHaveNonNumericValues_matcherFails() {
        assertFalse(matcher.matches(toHtml(TEXT_LIST)));
    }

    private final String[] TEXT_LIST =
            {"metric1 1", "metric2{name='red'} 23", "metric2{name='blue'} some-color"};
}
