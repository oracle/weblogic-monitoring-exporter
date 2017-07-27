package io.prometheus.wls.rest.domain;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 */
public class JsonPathMatcher {

    public static CombinablePathMatcher hasJsonPath(String jsonPath) {
        return new CombinablePathMatcher(jsonPath);
    }

    abstract static class JsonPathArrayMatcher extends TypeSafeDiagnosingMatcher<String> {
        private String jsonPath;
        private Set<String> expectedValues;

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

    public static class CombinablePathMatcher {
        private String jsonPath;

        CombinablePathMatcher(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        public JsonPathArrayMatcher withValues(String... expectedValue) {
            return new JsonPathWholeArrayMatcher(jsonPath, expectedValue);
        }

        JsonPathArrayMatcher includingValues(String... expectedValue) {
            return new JsonPathPartialArrayMatcher(jsonPath, expectedValue);
        }

    }
}
