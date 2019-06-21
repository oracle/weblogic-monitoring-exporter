package io.prometheus.wls.rest.matchers;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import org.junit.Test;

import java.util.Arrays;

import static io.prometheus.wls.rest.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Russell Gold
 */
public class PrometheusMetricsMatcherTest {

    private final PrometheusMetricsMatcher matcher = followsPrometheusRules();

    @Test
    public void whenMetricsAreGrouped_matcherPasses() throws Exception {
        assertTrue(matcher.matches(toHtml(ORDERED_LIST)));
    }

    private final String[] ORDERED_LIST =
            {"metric1 1", "metric2{name='red'} 23", "metric2{name='blue'} 34"};

    private String toHtml(String... metrics) {
        return Arrays.stream(metrics).map((s) -> s.replace('\'', '"')).collect(joining("\n"));
    }

    @Test
    public void whenMetricsAreInterspersed_matcherFails() throws Exception {
        assertFalse(matcher.matches(toHtml(MISORDERED_LIST)));
    }

    private final String[] MISORDERED_LIST =
            {"metric2{name='red'} 23", "metric1 1", "metric2{name='blue'} 34"};

    @Test
    public void whenMetricsHaveNonNumericValues_matcherFails() throws Exception {
        assertFalse(matcher.matches(toHtml(TEXT_LIST)));
    }

    private final String[] TEXT_LIST =
            {"metric1 1", "metric2{name='red'} 23", "metric2{name='blue'} some-color"};
}
