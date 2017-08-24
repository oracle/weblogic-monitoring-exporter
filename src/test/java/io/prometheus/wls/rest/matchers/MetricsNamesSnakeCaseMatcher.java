package io.prometheus.wls.rest.matchers;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import io.prometheus.wls.rest.domain.SnakeCaseUtil;
import org.hamcrest.Description;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Russell Gold
 */
public class MetricsNamesSnakeCaseMatcher extends org.hamcrest.TypeSafeDiagnosingMatcher<String> {

    public static MetricsNamesSnakeCaseMatcher usesSnakeCase() {
        return new MetricsNamesSnakeCaseMatcher();
    }

    @Override
    protected boolean matchesSafely(String metricsList, Description description) {
        String[] metrics = metricsList.split("\n");
        List<String> badNames = getNonSnakeCaseNames(metrics);
        if (badNames.isEmpty()) return true;

        description.appendValueList("found names not in snake case:[", ",", "]", badNames);
        return false;
    }

    private List<String> getNonSnakeCaseNames(String[] metrics) {
        return Arrays.stream(metrics)
                .filter((s) -> !s.startsWith("#"))
                .map(MetricsUtils::getMetricName)
                .filter((s) -> !SnakeCaseUtil.isCompliant(s))
                .filter(new MetricsUtils.Uniq())
                .collect(Collectors.toList());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("all metrics names in snake case");
    }

}
