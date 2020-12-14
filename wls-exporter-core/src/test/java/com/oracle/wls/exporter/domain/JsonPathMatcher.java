// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * A matcher for Json array-type that uses JsonPath queries.
 *
 * With this matcher, it is possible to define Hamcrest assertions such as:
 *
 *    assertThat(webClient.jsonQuery,
 *               hasJsonPath("$.children.serverRuntimes.children.groups.fields").withValues("name", "sample1"));
 *
 *    assertThat(querySpec(selector), hasJsonPath("$.fields").includingValues(MBeanSelector.TYPE_FIELD_NAME));
 *
 *    The difference in the query types is that "withValues" requires the specified values to be the full set
 *    of values in the array, while "includingValues" passes as long as all of those specified are present,
 *    even if the array contains other values.
 *
 * @author Russell Gold
 */
public class JsonPathMatcher {

    public static CombinablePathMatcher hasJsonPath(String jsonPath) {
        return new CombinablePathMatcher(jsonPath);
    }

    abstract static class JsonPathArrayMatcher extends TypeSafeDiagnosingMatcher<String> {
        private final String jsonPath;
        private final Set<String> expectedValues;

        JsonPathArrayMatcher(String jsonPath, String[] expectedValues) {
            this.jsonPath = jsonPath;
            this.expectedValues = new HashSet<>(Arrays.asList(expectedValues));
        }

        @Override
        protected boolean matchesSafely(String jsonString, Description description) {
            List<String> actualValues = JsonPath.read(jsonString, jsonPath);
            if (matches(new HashSet<>(actualValues), expectedValues)) return true;

            description.appendValue(actualValues);
            return false;
        }

        abstract boolean matches(Set<String> actualValues, Set<String> expectedValues);

        @Override
        public void describeTo(Description description) {
            description.appendValue(expectedValues).appendText(" at path ").appendValue(jsonPath);
        }
    }

    static class JsonPathWholeArrayMatcher extends JsonPathArrayMatcher {
        JsonPathWholeArrayMatcher(String jsonPath, String[] expectedValues) {
            super(jsonPath, expectedValues);
        }

        @Override
        boolean matches(Set<String> actualValues, Set<String> expectedValues) {
            return actualValues.equals(expectedValues);
        }
    }

    static class JsonPathPartialArrayMatcher extends JsonPathArrayMatcher {
        JsonPathPartialArrayMatcher(String jsonPath, String[] expectedValues) {
            super(jsonPath, expectedValues);
        }

        @Override
        boolean matches(Set<String> actualValues, Set<String> expectedValues) {
            return actualValues.containsAll(expectedValues);
        }
    }

    static class JsonPathStringMatcher extends TypeSafeDiagnosingMatcher<String> {
        private final String jsonPath;
        private final String expectedValue;

        JsonPathStringMatcher(String jsonPath, String expectedValue) {
            this.jsonPath = jsonPath;
            this.expectedValue = expectedValue;
        }

        @Override
        protected boolean matchesSafely(String jsonString, Description description) {
            String actualValue = JsonPath.read(jsonString, jsonPath);
            if (actualValue.equals(expectedValue)) return true;

            description.appendValue(actualValue);
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(expectedValue).appendText(" at path ").appendValue(jsonPath);
        }
    }

    static class JsonPathNumberMatcher extends TypeSafeDiagnosingMatcher<String> {
        private final String jsonPath;
        private final Number expectedValue;

        JsonPathNumberMatcher(String jsonPath, Number expectedValue) {
            this.jsonPath = jsonPath;
            this.expectedValue = expectedValue;
        }

        @Override
        protected boolean matchesSafely(String jsonString, Description description) {
            Number actualValue = JsonPath.read(jsonString, jsonPath);
            if (actualValue.equals(expectedValue)) return true;

            description.appendValue(actualValue);
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(expectedValue).appendText(" at path ").appendValue(jsonPath);
        }
    }

    public static class CombinablePathMatcher {
        private final String jsonPath;

        CombinablePathMatcher(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        public JsonPathNumberMatcher withValue(Number expectedValue) {
            return new JsonPathNumberMatcher(jsonPath, expectedValue);
        }

        public JsonPathStringMatcher withValue(String expectedValue) {
            return new JsonPathStringMatcher(jsonPath, expectedValue);
        }

        public JsonPathArrayMatcher withValues(String... expectedValue) {
            return new JsonPathWholeArrayMatcher(jsonPath, expectedValue);
        }

        JsonPathArrayMatcher includingValues(String... expectedValue) {
            return new JsonPathPartialArrayMatcher(jsonPath, expectedValue);
        }

    }
}
