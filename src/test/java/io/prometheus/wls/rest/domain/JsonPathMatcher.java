package io.prometheus.wls.rest.domain;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class JsonPathMatcher {

    static CombinablePathMatcher<? extends String[]> hasJsonPath(String jsonPath) {
        return new CombinablePathMatcher<>(jsonPath);
    }


    abstract static class JsonPathArrayMatcher extends TypeSafeDiagnosingMatcher<JsonQuerySpec> {
        private String jsonPath;
        private Set<String> expectedValues;

        JsonPathArrayMatcher(String jsonPath, String[] expectedValues) {
            this.jsonPath = jsonPath;
            this.expectedValues = new HashSet<>(Arrays.asList(expectedValues));
        }

        @Override
        protected boolean matchesSafely(JsonQuerySpec querySpec, Description description) {
            String jsonString = new Gson().toJson(querySpec);

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

    static class CombinablePathMatcher<X> {
        private String jsonPath;

        CombinablePathMatcher(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        JsonPathArrayMatcher withValues(String... expectedValue) {
            return new JsonPathWholeArrayMatcher(jsonPath, expectedValue);
        }

        JsonPathArrayMatcher includingValues(String... expectedValue) {
            return new JsonPathPartialArrayMatcher(jsonPath, expectedValue);
        }

    }
}
