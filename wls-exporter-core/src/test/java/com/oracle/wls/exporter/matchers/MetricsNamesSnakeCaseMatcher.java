// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.matchers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.wls.exporter.domain.SnakeCaseUtil;
import org.hamcrest.Description;

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
