// Copyright (c) 2019, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.Map;
import java.util.Objects;

import org.hamcrest.Description;

@SuppressWarnings("unused")
class MetricMatcher extends org.hamcrest.TypeSafeDiagnosingMatcher<Map<String,Object>> {
    private final String expectedKey;
    private final Object expectedValue;

    private MetricMatcher(String expectedKey, Object expectedValue) {
        this.expectedKey = expectedKey;
        this.expectedValue = expectedValue;
    }

    static MetricMatcher hasMetric(String expectedKey, Object expectedValue) {
        return new MetricMatcher(expectedKey, expectedValue);
    }

    @Override
    protected boolean matchesSafely(Map<String, Object> map, Description description) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (expectedKey.equals(entry.getKey()) && matchesValue(entry.getValue())) {
                return true;
            }
        }

        description.appendText("map was ").appendValueList("[", ", ", "]", map.entrySet());
        return false;
    }

    private boolean matchesValue(Object o) {
        if (expectedValue instanceof String)
            return Objects.equals(o, expectedValue);
        else if (expectedValue instanceof Integer)
            return ((Number) o).intValue() == ((Number) expectedValue).intValue();
        else if (expectedValue instanceof Double)
            return ((Number) o).doubleValue() == ((Number) expectedValue).doubleValue();
        else
            return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("map containing [")
                   .appendValue(expectedKey)
                   .appendText("->")
                   .appendValue(expectedValue)
                   .appendText("]");
    }
}
