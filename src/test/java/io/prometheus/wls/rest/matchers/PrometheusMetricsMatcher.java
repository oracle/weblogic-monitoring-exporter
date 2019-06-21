package io.prometheus.wls.rest.matchers;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.common.base.Strings;
import org.hamcrest.Description;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// verifies that metrics are grouped by name, ignores any comments
/**
 * @author Russell Gold
 */
public class PrometheusMetricsMatcher extends org.hamcrest.TypeSafeDiagnosingMatcher<String> {

    public static PrometheusMetricsMatcher followsPrometheusRules() {
        return new PrometheusMetricsMatcher();
    }

    @Override
    protected boolean matchesSafely(String metricsList, Description description) {
        String[] metrics = metricsList.split("\n");
        return allMetricsAreNumeric(description, metrics) && metricsInOrder(description, metrics);
    }

    private boolean allMetricsAreNumeric(Description description, String[] metrics) {
        for (String metric : metrics)
            if (hasNonNumericValue(metric)) return reportValueMismatch(metric, description);

        return true;
    }

    private boolean reportValueMismatch(String metric, Description description) {
        description.appendText("Non-numeric metric found at:\n  ");
        description.appendValue(metric);

        return false;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean hasNonNumericValue(String metric) {
        if (Strings.isNullOrEmpty(metric) || metric.trim().startsWith("#")) return false;
        if (!metric.contains(" ")) return false;

        String metricValue = metric.split(" ")[1];

        try {
            Double.parseDouble(metricValue.trim());
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private boolean metricsInOrder(Description description, String[] metrics) {
        String outOfOrderMetric = getOutOfOrderMetric(metrics);
        if (outOfOrderMetric == null) return true;

        description.appendText("out-of-order metric ").appendValue(outOfOrderMetric).appendText(" found at:\n");
        int i = 0;
        while (i < metrics.length && !startsWithMetricName(metrics[i], outOfOrderMetric)) i++;
        while (i < metrics.length && startsWithMetricName(metrics[i], outOfOrderMetric))
            description.appendText("  " + metrics[i++] + '\n');
        while (i < metrics.length && !startsWithMetricName(metrics[i], outOfOrderMetric))
            description.appendText("  " + metrics[i++] + '\n');

        if (i < metrics.length) description.appendText("  " + metrics[i] + '\n');
        return false;
    }

    private boolean startsWithMetricName(String line, String metricName) {
        return line.startsWith(metricName + ' ') || line.startsWith(metricName + '{');
    }

    private String getOutOfOrderMetric(String[] split) {
        List<String> groups = getSortedGroups(split);
        if (groups.size() == 0) return null;

        String last = groups.get(0);
        for (int i = 1; i < groups.size(); i++) {
            String current = groups.get(i);
            if (last.equals(current))
                return current;
            else
                last = current;
        }

        return null;
    }

    private List<String> getSortedGroups(String[] metricsList) {
        return Arrays.stream(metricsList)
                .filter((s) -> !s.startsWith("#"))
                .map(MetricsUtils::getMetricName)
                .filter(new MetricsUtils.Uniq())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Prometheus-compliant metrics list");
    }
}
