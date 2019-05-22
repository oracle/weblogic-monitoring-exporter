// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package io.prometheus.wls.rest.domain;

import org.hamcrest.Description;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
class MetricMatcher extends org.hamcrest.TypeSafeDiagnosingMatcher<Map<String,Object>> {
    private String expectedKey;
    private Object expectedValue;

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
