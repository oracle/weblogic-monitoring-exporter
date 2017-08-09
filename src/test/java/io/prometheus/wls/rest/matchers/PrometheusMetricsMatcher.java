package io.prometheus.wls.rest.matchers;

import org.hamcrest.Description;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// verifies that metrics are grouped by name, ignores any comments
public class PrometheusMetricsMatcher extends org.hamcrest.TypeSafeDiagnosingMatcher<String> {

    public static PrometheusMetricsMatcher followsPrometheusRules() {
        return new PrometheusMetricsMatcher();
    }
    @Override
    protected boolean matchesSafely(String metricsList, Description description) {
        String[] metrics = metricsList.split("\n");
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
                .map(PrometheusMetricsMatcher::getMetricName)
                .filter(new Uniq())
                .sorted()
                .collect(Collectors.toList());
    }

    private static String duplicate(String s1, String s2) {
        String result = s1 == null || !s1.equals(s2) ? null : s2;
        System.out.printf("Input %s and %s -> result %s%n", s1, s2, result);
        return result;
    }

    private static String getMetricName(String s) {
        String spec = s.trim().split(" ")[0];
        return spec.contains("{") ? spec.split("\\{")[0] : spec;
    }

    static class Uniq implements Predicate<String> {
        String last = "";
        @Override
        public boolean test(String s) {
            try {
                return s != null && !s.equals(last);
            } finally {
                last = s;
            }
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Prometheus-compliant metrics list");
    }
}
