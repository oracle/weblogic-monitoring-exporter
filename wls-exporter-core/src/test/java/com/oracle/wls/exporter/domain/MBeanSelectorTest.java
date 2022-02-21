// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.domain.JsonPathMatcher.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

/**
 * @author Russell Gold
 */
public class MBeanSelectorTest {

    private static final String EXPECTED_TYPE = "WebAppComponentRuntime";
    private static final String EXPECTED_PREFIX = "webapp_";
    private static final String EXPECTED_KEY = "servletName";
    private static final String EXPECTED_KEY_NAME = "config";
    private static final String[] EXPECTED_VALUES = {"first", "second", "third"};
    private static final String[] EXPECTED_COMPONENT_VALUES = {"age", "beauty"};

    @Test
    public void byDefault_useRuntimeRestUrl() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getUrl(Protocol.HTTP, "myhost", 1234),
                   equalTo(String.format(QueryType.RUNTIME_URL_PATTERN, "http", "myhost", 1234)));
    }

    @Test
    public void whenConfigurationQuerySpecified_useConfigurationRestUrl() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());
        selector.setQueryType(QueryType.CONFIGURATION);

        assertThat(selector.getUrl(Protocol.HTTP, "myhost", 1234),
                   equalTo(String.format(QueryType.CONFIGURATION_URL_PATTERN, "http", "myhost", 1234)));
    }

    @Test
    public void whenNoTypeInMap_selectorHasNoType() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getType(), emptyOrNullString());
    }

    @Test
    public void whenMapHasType_selectorHasType() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.TYPE, EXPECTED_TYPE));

        assertThat(selector.getType(), equalTo(EXPECTED_TYPE));
    }

    @Test
    public void whenNoPrefixInMap_selectorHasNoPrefix() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getPrefix(), emptyOrNullString());
    }

    @Test
    public void whenMapHasPrefix_selectorHasPrefix() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.PREFIX, EXPECTED_PREFIX));

        assertThat(selector.getPrefix(), equalTo(EXPECTED_PREFIX));
    }

    @Test
    public void whenNoKeyInMap_selectorHasNoKey() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getKey(), emptyOrNullString());
    }

    @Test
    public void whenMapHasKey_selectorHasKey() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY));

        assertThat(selector.getKey(), equalTo(EXPECTED_KEY));
    }

    @Test
    public void whenNoKeyNameInMap_selectorHasNoKeyName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getKeyName(), emptyOrNullString());
    }

    @Test
    public void whenMapHasKeyName_selectorHasKeyName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.KEY_NAME, EXPECTED_KEY_NAME));

        assertThat(selector.getKeyName(), equalTo(EXPECTED_KEY_NAME));
    }

    @Test
    public void whenMapHasKeyNameButNoKeyName_selectorUsesKeyAsName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY));

        assertThat(selector.getKeyName(), equalTo(EXPECTED_KEY));
    }

    @Test
    public void whenMapHasBothKeyAndKeyName_selectorUsesKeyName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY,
                                                                      MBeanSelector.KEY_NAME, EXPECTED_KEY_NAME));

        assertThat(selector.getKeyName(), equalTo(EXPECTED_KEY_NAME));
    }

    @Test
    public void whenNoValuesInMap_selectorHasNoValues() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getValues(), emptyArray());
    }

    @Test
    public void whenMapHasValues_selectorHasValues() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.VALUES, EXPECTED_VALUES));

        assertThat(selector.getValues(), equalTo(EXPECTED_VALUES));
    }


    @Test
    public void whenNoNestedSelectorsInMap_selectorHasNoNestedSelectors() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getNestedSelectors(), anEmptyMap());
    }


    @Test
    public void whenMapHasNestedSelector_createInParent() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("servlets",
                getServletMap()));

        MBeanSelector servlets = selector.getNestedSelectors().get("servlets");
        assertThat(servlets.getKey(), equalTo(EXPECTED_KEY));
        assertThat(servlets.getValues(), equalTo(EXPECTED_VALUES));

    }

    private Map<String, Serializable> getServletMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY, MBeanSelector.VALUES, EXPECTED_VALUES);
    }

    @Test
    public void queryFieldsMatchValues() {
        MBeanSelector selector = MBeanSelector.create(
                ImmutableMap.of(MBeanSelector.VALUES, EXPECTED_COMPONENT_VALUES));

        assertThat(querySpec(selector), hasJsonPath("$.fields").withValues(EXPECTED_COMPONENT_VALUES));
    }

    private static String querySpec(MBeanSelector selector) {
        return new Gson().toJson(selector.toQuerySpec());
    }

    @Test
    public void whenKeySpecified_isIncludedInQueryFields() {
        MBeanSelector selector = MBeanSelector.create(
                ImmutableMap.of(MBeanSelector.VALUES, EXPECTED_COMPONENT_VALUES, MBeanSelector.QUERY_KEY, "name"));

        assertThat(querySpec(selector), hasJsonPath("$.fields").includingValues("name"));
    }

    @Test
    public void whenTypeSpecified_standardFieldTypeIsIncludedInQueryFields() {
        MBeanSelector selector = MBeanSelector.create(
                ImmutableMap.of(MBeanSelector.VALUES, EXPECTED_COMPONENT_VALUES, MBeanSelector.TYPE, "OneTypeOnly"));

        assertThat(querySpec(selector), hasJsonPath("$.fields").includingValues(MBeanSelector.TYPE_FIELD_NAME));
    }

    @Test
    public void whenMapHasNestedElements_pathIncludesChildren() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.VALUES, new String[] {"first", "second"})));

        assertThat(querySpec(selector), hasJsonPath("$.children.servlets.fields").withValues("first", "second"));
    }

    @Test
    public void whenMergingLeafElements_combineValues() {
        MBeanSelector selector1 = createLeaf("first", "second");
        MBeanSelector selector2 = createLeaf("second", "third");

        assertThat(querySpec(selector1.merge(selector2)), hasJsonPath("$.fields").withValues("first", "second", "third"));
    }

    @Test
    public void whenLeafElementsHaveMatchingAttributes_mayCombine() {
        MBeanSelector selector1 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers", "first", "second");
        MBeanSelector selector2 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers", "second", "third");

        assertThat(selector1.mayMergeWith(selector2), is(true));
    }

    @Test
    public void whenLeafElementsHaveMatchingAttributes_mergedResultHasOriginalAttributes() {
        MBeanSelector selector1 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers", "first", "second");
        MBeanSelector selector2 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers", "second", "third");

        MBeanSelector result = selector1.merge(selector2);
        assertThat(result.getType(), equalTo("Type1"));
        assertThat(result.getPrefix(), equalTo("#_"));
        assertThat(result.getKey(), equalTo("name"));
        assertThat(result.getKeyName(), equalTo("numbers"));
    }

    @Test
    public void whenLeafElementsHaveMisMatchedAttributes_mayNotCombine() {
        assertThat(createLeaf("keyName:numbers", "first", "second").mayMergeWith(createLeaf("second", "third")), is(false));
        assertThat(createLeaf("prefix:_", "key:Name").mayMergeWith(createLeaf("prefix:_", "aValue")), is(false));
        assertThat(createLeaf("key:_", "type:Name").mayMergeWith(createLeaf("key:_", "type:color")), is(false));
        assertThat(createLeaf("prefix:__").mayMergeWith(createLeaf("prefix:asdf")), is(false));
    }

    private MBeanSelector createLeaf(String... params) {
        Map<String, Object> map = new HashMap<>();
        int i = 0;
        while (i < params.length) {
            if (!params[i].contains(":")) break;
            String[] split = params[i++].split(":");
            map.put(split[0], split[1]);
        }
        if (i < params.length)
            map.put(MBeanSelector.VALUES, Arrays.copyOfRange(params, i, params.length));
        return MBeanSelector.create(map);
    }

    @Test
    public void whenSelectorsNoCommonNestedElementsWithSameName_mayMerge() {
        MBeanSelector selector1 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES, new String[] {"first", "second"})));
        MBeanSelector selector2 = MBeanSelector.create(ImmutableMap.of("kidlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES, new String[] {"first", "second"})));

        assertThat(selector1.mayMergeWith(selector2), is(true));
    }

    @Test
    public void whenSelectorsHaveMismatchedNestedElementsWithSameName_mayNotMerge() {
        MBeanSelector selector1 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES, new String[] {"first", "second"})));
        MBeanSelector selector2 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES, new String[] {"first", "second"})));

        assertThat(selector1.mayMergeWith(selector2), is(false));
    }

    @Test
    public void whenSelectorsHaveMismatchedNestedElementsWithDifferentName_merge() {
        MBeanSelector selector1 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES, new String[] {"first", "second"})));
        MBeanSelector selector2 = MBeanSelector.create(ImmutableMap.of("ejbs",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES, new String[] {"first", "second"})));
        MBeanSelector result = selector1.merge(selector2);

        assertThat(result.getNestedSelectors(), both(hasKey("servlets")).and(hasKey("ejbs")));
    }

    @Test
    public void whenSelectorsHaveDeeplyNestedElementsWithDifferentName_mayMerge() {
        MBeanSelector selector1 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("servlets",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES, new String[] {"first", "second"}))));
        MBeanSelector selector2 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("ejbs",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES, new String[] {"first", "second"}))));

        assertThat(selector1.mayMergeWith(selector2), is(true));
    }

    @Test
    public void whenSelectorsHaveDeeplyNestedElementsWithDifferentName_merge() {
        MBeanSelector selector1 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("servlets",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES, new String[] {"first", "second"}))));
        MBeanSelector selector2 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("ejbs",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES, new String[] {"first", "second"}))));
        MBeanSelector result = selector1.merge(selector2);

        assertThat(result.getNestedSelectors(), aMapWithSize(1));
        assertThat(result.getNestedSelectors().get("components").getNestedSelectors(), both(hasKey("servlets")).and(hasKey("ejbs")));
    }

    @Test
    public void generateJsonRequest() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("applicationRuntimes", getApplicationMap()));
        JsonParser parser = new JsonParser();

        assertThat(parser.parse(selector.getRequest()), equalTo(parser.parse(compressedJsonForm(EXPECTED_JSON_REQUEST))));
    }

    private Map<String, Object> getApplicationMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name",
                               "componentRuntimes", getComponentMap());
    }

    private Map<String, Object> getComponentMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name", MBeanSelector.VALUES, EXPECTED_COMPONENT_VALUES,
                               "servlets", getServletMap());
    }

    @Test
    public void domainNameSelector_selectsConfigurationUrl() {
        assertThat(MBeanSelector.DOMAIN_NAME_SELECTOR.getUrl(Protocol.HTTP, "myhost", 1234),
                   equalTo(String.format(QueryType.CONFIGURATION_URL_PATTERN, "http", "myhost", 1234)));
    }

    @Test
    public void domainNameSelector_requestsName() {
        assertThat(MBeanSelector.DOMAIN_NAME_SELECTOR.getValues(), arrayContaining("name"));
    }

    // This lets us simplify the creation of string to match the full request. All white space is removed and
    // single quotes are converted to double quotes, to match the actual format generated by Gson.
    private String compressedJsonForm(String jsonRequest) {
        StringBuilder sb = new StringBuilder();
        for (char c : jsonRequest.toCharArray())
            if (c == '\'')
                sb.append('"');
            else if (!Character.isWhitespace(c))
                sb.append(c);

        return sb.toString();
    }

    private static final String EXPECTED_JSON_REQUEST =
            "{\n" +
            "  'links' : [], 'fields' : [],\n" +
            "  'children': {\n" +
            "     'applicationRuntimes': {\n" +
            "        'links': [], 'fields': [ 'name' ],\n" +
            "        'children': {\n" +
            "           'componentRuntimes': {\n" +
            "              'links': [],\n" +
            "              'fields': ['name', 'age', 'beauty'],\n" +
            "              'children': {\n" +
            "                 'servlets': {\n" +
            "                  'links': [],\n" +
            "                  'fields': ['servletName', 'first', 'second', 'third']\n" +
            "                 }\n" +  // servlets
            "              }\n" + // componentRuntimes.children
            "           }\n" +  // componentRuntimes
            "        }\n" + // applicationRuntimes.children
            "     }\n" + // applicationRuntimes
            "  }\n" + // .children
            "}";

    @Test
    public void whenNoValuesListedForSerlvets_generateJsonRequest() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("applicationRuntimes", getNoServletValuesApplicationMap()));
        JsonParser parser = new JsonParser();

        assertThat(parser.parse(selector.getRequest()), equalTo(parser.parse(compressedJsonForm(EXPECTED_ALL_SERVLET_VALUES_JSON_REQUEST))));
    }

    private Map<String, Object> getNoServletValuesApplicationMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name",
                               "componentRuntimes", getNoServletValuesComponentMap());
    }

    private Map<String, Object> getNoServletValuesComponentMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name", MBeanSelector.VALUES, EXPECTED_COMPONENT_VALUES,
                               "servlets", getNoValuesServletMap());
    }

    private Map<String, Serializable> getNoValuesServletMap() {
        return ImmutableMap.of(MBeanSelector.PREFIX, "servlet_", MBeanSelector.QUERY_KEY, EXPECTED_KEY);
    }

    private static final String EXPECTED_ALL_SERVLET_VALUES_JSON_REQUEST =
            "{\n" +
            "  'links' : [], 'fields' : [],\n" +
            "  'children': {\n" +
            "     'applicationRuntimes': {\n" +
            "        'links': [], 'fields': [ 'name' ],\n" +
            "        'children': {\n" +
            "           'componentRuntimes': {\n" +
            "              'links': [],\n" +
            "              'fields': ['name', 'age', 'beauty'],\n" +
            "              'children': {\n" +
            "                 'servlets': {\n" +
            "                  'links': []\n" +
            "                 }\n" +  // servlets
            "              }\n" + // componentRuntimes.children
            "           }\n" +  // componentRuntimes
            "        }\n" + // applicationRuntimes.children
            "     }\n" + // applicationRuntimes
            "  }\n" + // .children
            "}";

    // todo - should we allow multiple types in a single filter?

}
