// Copyright (c) 2017, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.matchers;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Russell Gold
 */
class PrometheusMetricsMatcherTest {

    private final PrometheusMetricsMatcher matcher = followsPrometheusRules();

    @Test
    void whenMetricsAreGrouped_matcherPasses() {
        assertTrue(matcher.matches(toHtml(orderedList)));
    }

    private final String[] orderedList =
            {"metric1 1", "metric2{name='red'} 23", "metric2{name='blue'} 34"};

    private String toHtml(String... metrics) {
        return Arrays.stream(metrics).map(s -> s.replace('\'', '"')).collect(joining("\n"));
    }

    @Test
    void whenMetricsAreInterspersed_matcherFails() {
        assertFalse(matcher.matches(toHtml(misorderedList)));
    }

    private final String[] misorderedList =
            {"metric2{name='red'} 23", "metric1 1", "metric2{name='blue'} 34"};

    @Test
    void whenMetricsHaveNonNumericValues_matcherFails() {
        assertFalse(matcher.matches(toHtml(textList)));
    }

    private final String[] textList =
            {"metric1 1", "metric2{name='red'} 23", "metric2{name='blue'} some-color"};
}
