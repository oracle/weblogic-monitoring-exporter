// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.meterware.simplestub.Stub;
import com.oracle.wls.exporter.ClockStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.gson.JsonParser.parseString;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Russell Gold
 */
class MBeanSelectorTest {

    private static final String EXPECTED_TYPE = "WebAppComponentRuntime";
    private static final String EXPECTED_PREFIX = "webapp_";
    private static final String EXPECTED_KEY = "servletName";
    private static final String EXPECTED_KEY_NAME = "config";
    private static final String[] EXPECTED_VALUES = {"first", "second", "third"};
    private static final String[] EXPECTED_COMPONENT_VALUES = {"age", "beauty"};

    private final List<Memento> mementos = new ArrayList<>();
    private final ClockStub clockStub = Stub.createStub(ClockStub.class);

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        mementos.add(StaticStubSupport.install(MBeanSelector.class, "systemClock", clockStub));
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    void byDefault_useRuntimeRestUrl() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getUrl(Protocol.HTTP, "myhost", 1234),
                   equalTo(String.format(QueryType.RUNTIME_URL_PATTERN, "http", "myhost", 1234)));
    }

    @Test
    void whenConfigurationQuerySpecified_useConfigurationRestUrl() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());
        selector.setQueryType(QueryType.CONFIGURATION);

        assertThat(selector.getUrl(Protocol.HTTP, "myhost", 1234),
                   equalTo(String.format(QueryType.CONFIGURATION_URL_PATTERN, "http", "myhost", 1234)));
    }

    @Test
    void whenNoTypeInMap_selectorHasNoType() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getType(), emptyOrNullString());
    }

    @Test
    void whenMapHasType_selectorHasType() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.TYPE_KEY, EXPECTED_TYPE));

        assertThat(selector.getType(), equalTo(EXPECTED_TYPE));
    }

    @Test
    void whenNoPrefixInMap_selectorHasNoPrefix() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getPrefix(), emptyOrNullString());
    }

    @Test
    void whenMapHasPrefix_selectorHasPrefix() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.PREFIX_KEY, EXPECTED_PREFIX));

        assertThat(selector.getPrefix(), equalTo(EXPECTED_PREFIX));
    }

    @Test
    void whenNoKeyInMap_selectorHasNoKey() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getKey(), emptyOrNullString());
    }

    @Test
    void whenMapHasKey_selectorHasKey() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY));

        assertThat(selector.getKey(), equalTo(EXPECTED_KEY));
    }

    @Test
    void whenNoKeyNameInMap_selectorHasNoKeyName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getKeyName(), emptyOrNullString());
    }

    @Test
    void whenMapHasKeyName_selectorHasKeyName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.KEY_NAME, EXPECTED_KEY_NAME));

        assertThat(selector.getKeyName(), equalTo(EXPECTED_KEY_NAME));
    }

    @Test
    void whenMapHasKeyNameButNoKeyName_selectorUsesKeyAsName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY));

        assertThat(selector.getKeyName(), equalTo(EXPECTED_KEY));
    }

    @Test
    void whenMapHasBothKeyAndKeyName_selectorUsesKeyName() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY,
                                                                      MBeanSelector.KEY_NAME, EXPECTED_KEY_NAME));

        assertThat(selector.getKeyName(), equalTo(EXPECTED_KEY_NAME));
    }

    @Test
    void whenNoValuesInMap_selectorHasNoValues() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getQueryValues(), emptyArray());
    }

    @Test
    void whenMapHasValues_selectorHasValues() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.VALUES_KEY, EXPECTED_VALUES));

        assertThat(selector.getQueryValues(), equalTo(EXPECTED_VALUES));
    }
    
    @Test
    void whenNoNestedSelectorsInMap_selectorHasNoNestedSelectors() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of());

        assertThat(selector.getNestedSelectors(), anEmptyMap());
    }

    @Test
    void whenMapHasNestedSelector_createInParent() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("servlets", getServletMap()));

        MBeanSelector servlets = selector.getNestedSelectors().get("servlets");
        assertThat(servlets.getKey(), equalTo(EXPECTED_KEY));
        assertThat(servlets.getQueryValues(), equalTo(EXPECTED_VALUES));

    }

    private Map<String, Serializable> getServletMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, EXPECTED_KEY, MBeanSelector.VALUES_KEY, EXPECTED_VALUES);
    }

    @Test
    void queryFieldsMatchValues() {
        MBeanSelector selector = MBeanSelector.create(
                ImmutableMap.of(MBeanSelector.VALUES_KEY, EXPECTED_COMPONENT_VALUES));
        assertThat(querySpec(selector), hasJsonPath("$.fields", contains(EXPECTED_COMPONENT_VALUES)));
    }

    private static String querySpec(MBeanSelector selector) {
        return selector.getRequest();
    }

    @Test
    void whenKeySpecified_isIncludedInQueryFields() {
        MBeanSelector selector = MBeanSelector.create(
                ImmutableMap.of(MBeanSelector.VALUES_KEY, EXPECTED_COMPONENT_VALUES, MBeanSelector.QUERY_KEY, "name"));

        assertThat(querySpec(selector), hasJsonPath("$.fields", hasItem("name")));
    }

    @Test
    void whenTypeSpecified_standardFieldTypeIsIncludedInQueryFields() {
        MBeanSelector selector = MBeanSelector.create(
                ImmutableMap.of(MBeanSelector.VALUES_KEY, EXPECTED_COMPONENT_VALUES, MBeanSelector.TYPE_KEY, "OneTypeOnly"));

        assertThat(querySpec(selector), hasJsonPath("$.fields", hasItem(MBeanSelector.TYPE_FIELD_NAME)));
    }

    @Test
    void whenMapHasNestedElements_pathIncludesChildren() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));

        assertThat(querySpec(selector), hasJsonPath("$.children.servlets.fields", containsInAnyOrder("first", "second")));
    }

    @Test
    void whenTopLevelSelectorHasFields_isIncludedInQuery() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.VALUES_KEY, "sequence"));

        assertThat(querySpec(selector), hasJsonPath("$.fields", contains("sequence")));
    }

    @Test
    void whenTopLevelSelectorHasNoFields_fieldsListIsEmpty() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));

        assertThat(querySpec(selector), hasJsonPath("$.fields", hasSize(0)));
    }

    @Test
    void whenTopLevelSelectorHasPrefixAndNoFields_fieldsListIsEmpty() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of(MBeanSelector.PREFIX_KEY, "top_", "servlets",
                ImmutableMap.of(MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));

        assertThat(querySpec(selector), hasJsonPath("$.fields", hasSize(0)));
    }

    @Test
    void whenIncludedKeysSpecifiedWithoutKeyName_report() {
        final Map<String, Object> BAD_MAP_WITH_INCLUDED_KEYS
              = ImmutableMap.of("group", ImmutableMap.of(MBeanSelector.INCLUDED_KEYS_KEY, "a"));
        
        assertThrows(ConfigurationException.class, () -> MBeanSelector.create(BAD_MAP_WITH_INCLUDED_KEYS));
    }

    @Test
    void whenExcludedKeysSpecifiedWithoutKeyName_report() {
        final Map<String, Object> BAD_MAP_WITH_EXCLUDED_KEYS
              = ImmutableMap.of("group", ImmutableMap.of(MBeanSelector.EXCLUDED_KEYS_KEY, "a"));

        assertThrows(ConfigurationException.class, () -> MBeanSelector.create(BAD_MAP_WITH_EXCLUDED_KEYS));
    }

    @Test
    void whenMapHasSelectedKeys_AddToKeyQuery() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_INCLUDED_KEYS);

        assertThat(selector.getKeyRequest(), hasJsonPath("$.children.servlets.fields", contains("servletName")));
    }

    private static final Map<String, Object> MAP_WITH_INCLUDED_KEYS = ImmutableMap.of("servlets",
          ImmutableMap.of(MBeanSelector.QUERY_KEY, "servletName", MBeanSelector.INCLUDED_KEYS_KEY, "alpha|beta"));

    @Test
    void whenMapHasSelectedKeysAtMultipleLevels_AddToKeyQuery() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITH_NESTED_INCLUDED_KEYS);

        assertThat(selector.getKeyRequest(), hasJsonPath("$.children.groups.fields", contains("groupName")));
        assertThat(selector.getKeyRequest(), hasJsonPath("$.children.groups.children.middle.children.subgroup1.fields", contains("name1")));
        assertThat(selector.getKeyRequest(), hasJsonPath("$.children.groups.children.middle.children.subgroup2.fields", contains("name2")));
    }

    private static final Map<String, Object> DEEP_MAP_WITH_NESTED_INCLUDED_KEYS = ImmutableMap.of(MBeanSelector.PREFIX_KEY, "wls_", "groups",
        ImmutableMap.of(MBeanSelector.QUERY_KEY, "groupName", MBeanSelector.INCLUDED_KEYS_KEY, "alpha|beta",
            "middle", ImmutableMap.of(
                "subgroup1", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name1", MBeanSelector.INCLUDED_KEYS_KEY, "abc.*", MBeanSelector.VALUES_KEY, "group1Val" ),
                "subgroup2", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name2", MBeanSelector.INCLUDED_KEYS_KEY, "def.*", MBeanSelector.VALUES_KEY, "group2Val" ))));

    @Test
    void whenIntermediateLevelLacksSelectedKeys_dontRequestFieldsInKeyQuery() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITH_NESTED_INCLUDED_KEYS);

        assertThat(selector.getKeyRequest(), hasJsonPath("$.children.groups.children.middle.fields", hasSize(0)));
    }

    @Test
    void whenNestedEntriesLackFilter_excludeSubTreeFromKeyQuery() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITHOUT_NESTED_INCLUDED_KEYS);

        assertThat(selector.getKeyRequest(), hasJsonPath("$.children.groups.fields", contains("groupName")));
        assertThat(selector.getKeyRequest(), hasNoJsonPath("$.children.groups.children"));
    }

    private static final Map<String, Object> DEEP_MAP_WITHOUT_NESTED_INCLUDED_KEYS = ImmutableMap.of("groups",
        ImmutableMap.of(MBeanSelector.QUERY_KEY, "groupName", MBeanSelector.INCLUDED_KEYS_KEY, "alpha|beta",
            "middle", ImmutableMap.of(
                "subgroup1", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name1", MBeanSelector.VALUES_KEY, "group1Val" ),
                "subgroup2", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name2", MBeanSelector.VALUES_KEY, "group2Val" ))));

    @Test
    void whenMapLacksKeyFilter_dontNeedNewKeys() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "servletName")));

        assertThat(selector.needsNewKeys(), is(false));
    }

    @Test
    void whenMapHasIncludedKeysAndSelectorLacksFoundKeys_needNewKeys() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_INCLUDED_KEYS);

        assertThat(selector.needsNewKeys(), is(true));
    }

    @Test
    void whenMapHasUpToDateFoundKeys_dontNeedNewKeys() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_INCLUDED_KEYS);
        selector.offerKeys(KEY_RESPONSE);

        assertThat(selector.needsNewKeys(), is(false));
    }

    private static final String KEY_RESPONSE_JSON = "{\"servlets\": {\"items\": [\n" +
                "     {\"servletName\": \"alpha\"},\n" +
                "     {\"servletName\": \"beta\" },\n" +
                "     {\"servletName\": \"gamma\"}\n" +
                "]}}";

    private static final JsonObject KEY_RESPONSE =
          JsonParser.parseString(KEY_RESPONSE_JSON).getAsJsonObject();

    @Test
    void whenMapHasOutOfDateFoundKeys_needNewKeys() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_INCLUDED_KEYS);
        selector.offerKeys(KEY_RESPONSE);
        clockStub.incrementSeconds(MBeanSelector.KEY_UPDATE_INTERVAL_SECONDS);

        assertThat(selector.needsNewKeys(), is(true));
    }

    @Test
    void afterKeysOffered_selectorHasIncludedKeys() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_INCLUDED_KEYS);
        selector.offerKeys(KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.children.servlets.servletName", containsInAnyOrder("alpha", "beta")));
    }

    @Test
    void afterKeysOfferedThatDoNotMatchFilter_parentSelectorHasNoChildren() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_INCLUDED_KEYS);
        selector.offerKeys(MISMATCHED_KEY_RESPONSE);

        assertThat(selector.getRequest(), hasNoJsonPath("$.children"));
    }

    @Test
    void afterKeysOfferedThatDoNotMatchFilter_parentSelectorHasEmptyFieldsArray() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_INCLUDED_KEYS);
        selector.offerKeys(MISMATCHED_KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.fields", hasSize(0)));
    }

    private static final String MISMATCHED_KEY_RESPONSE_JSON = "{\"servlets\": {\"items\": [\n" +
                "     {\"servletName\": \"delta\"},\n" +
                "     {\"servletName\": \"epsilon\" },\n" +
                "     {\"servletName\": \"zeta\"}\n" +
                "]}}";

    private static final JsonObject MISMATCHED_KEY_RESPONSE =
          JsonParser.parseString(MISMATCHED_KEY_RESPONSE_JSON).getAsJsonObject();

    @Test
    void whenMapHasSelectedKeysAtMultipleLevels_specifySelectedKeys() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITH_INCLUDED_KEYS);
        selector.offerKeys(DEEP_KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.groupName", containsInAnyOrder("alpha", "beta")));
        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.children.subgroup1.name1", containsInAnyOrder("abcdef", "abc123", "abc567")));
        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.children.subgroup2.name2", containsInAnyOrder("defabc", "def123", "def678")));
    }

    private static final Map<String, Object> DEEP_MAP_WITH_INCLUDED_KEYS = ImmutableMap.of("groups",
          ImmutableMap.of(MBeanSelector.QUERY_KEY, "groupName", MBeanSelector.INCLUDED_KEYS_KEY, "alpha|beta",
                "subgroup1", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name1", MBeanSelector.INCLUDED_KEYS_KEY, "abc.*", MBeanSelector.VALUES_KEY, "group1Val" ),
                "subgroup2", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name2", MBeanSelector.INCLUDED_KEYS_KEY, "def.*", MBeanSelector.VALUES_KEY, "group2Val" )));

    private static final String DEEP_KEY_RESPONSE_JSON = "{'groups': {'items': [\n" +
          "     {'groupName': 'alpha',\n" +
          "      'subgroup1': {'items': [\n" +
          "          {'name1': 'abcdef'},\n" +
          "          {'name1': 'abc123'},\n" +
          "          {'name1': 'ab12_2'}\n" +
          "       ]},\n" +
          "      'subgroup2': {'items': [\n" +
          "          {'name2': 'abcdef'},\n" +
          "          {'name2': 'defabc'},\n" +
          "          {'name2': 'def123'}\n" +
          "       ]}\n" +
          "     },\n" +
          "     {'groupName': 'beta',\n" +
          "      'subgroup1': {'items': [\n" +
          "          {'name1': 'abcdef'},\n" +
          "          {'name1': 'abc567'},\n" +
          "          {'name1': 'abjkl'}\n" +
          "       ]},\n" +
          "      'subgroup2': {'items': [\n" +
          "          {'name2': 'def678'},\n" +
          "       ]}\n" +
          "     },\n" +
          "     {'groupName': 'gamma',\n" +
          "      'subgroup1': {'items': [\n" +
          "          {'name1': 'abcxyz'},\n" +
          "       ]},\n" +
          "      'subgroup2': {'items': [\n" +
          "          {'name2': 'def987'},\n" +
          "       ]}\n" +
          "     }\n" +
          "]}}";

    private static final JsonObject DEEP_KEY_RESPONSE =
          JsonParser.parseString(DEEP_KEY_RESPONSE_JSON.replace("'", "\"")).getAsJsonObject();

    @Test
    void whenMapHasMismatchedNested_specifySelectedKeys() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITH_INCLUDED_KEYS);
        selector.offerKeys(MISMATCHED_DEEP_KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.groupName", containsInAnyOrder("alpha", "beta")));
        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.children.subgroup1.name1", containsInAnyOrder("abcdef", "abc123", "abc567")));
        assertThat(selector.getRequest(), hasNoJsonPath("$.children.groups.children.subgroup2"));
    }

    private static final String MISMATCHED_DEEP_KEY_RESPONSE_JSON = "{'groups': {'items': [\n" +
          "     {'groupName': 'alpha',\n" +
          "      'subgroup1': {'items': [\n" +
          "          {'name1': 'abcdef'},\n" +
          "          {'name1': 'abc123'},\n" +
          "          {'name1': 'ab12_2'}\n" +
          "       ]},\n" +
          "      'subgroup2': {'items': [\n" +
          "          {'name2': 'abcdef'},\n" +
          "          {'name2': 'xyzabc'},\n" +
          "          {'name2': 'jklmn'}\n" +
          "       ]}\n" +
          "     },\n" +
          "     {'groupName': 'beta',\n" +
          "      'subgroup1': {'items': [\n" +
          "          {'name1': 'abcdef'},\n" +
          "          {'name1': 'abc567'},\n" +
          "          {'name1': 'abjkl'}\n" +
          "       ]},\n" +
          "      'subgroup2': {'items': [\n" +
          "          {'name2': 'ghi678'},\n" +
          "       ]}\n" +
          "     },\n" +
          "     {'groupName': 'gamma',\n" +
          "      'subgroup1': {'items': [\n" +
          "          {'name1': 'abcxyz'},\n" +
          "       ]},\n" +
          "      'subgroup2': {'items': [\n" +
          "          {'name2': 'jkl987'},\n" +
          "       ]}\n" +
          "     }\n" +
          "]}}";

    private static final JsonObject MISMATCHED_DEEP_KEY_RESPONSE =
          JsonParser.parseString(MISMATCHED_DEEP_KEY_RESPONSE_JSON.replace("'", "\"")).getAsJsonObject();

    @Test
    void whenMapHasBothIncludedAndExcludedKeys_selectKeysLeft() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITH_INCLUDED_AND_EXCLUDED_KEYS);
        selector.offerKeys(DEEP_KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.children.subgroup1.name1", containsInAnyOrder("abc123", "abc567", "abcxyz")));
    }

    private static final Map<String, Object> DEEP_MAP_WITH_INCLUDED_AND_EXCLUDED_KEYS = ImmutableMap.of("groups",
          ImmutableMap.of(MBeanSelector.QUERY_KEY, "groupName",
                "subgroup1", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name1", MBeanSelector.INCLUDED_KEYS_KEY, "abc.*", MBeanSelector.EXCLUDED_KEYS_KEY, "abcd.*" ),
                "subgroup2", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name2", MBeanSelector.VALUES_KEY, "group2Val" )));

    @Test
    void whenParentHasIncludedKeysButChildDoesNot_doNotSpecifyNamesForChild() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITH_ASSYMETRIC_KEYS);
        selector.offerKeys(DEEP_KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.groupName", containsInAnyOrder("alpha", "beta")));
        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.children.subgroup1.name1", containsInAnyOrder("abcdef", "abc123", "abc567")));
        assertThat(selector.getRequest(), hasNoJsonPath("$.children.groups.children.subgroup2.name2"));
    }

    private static final Map<String, Object> DEEP_MAP_WITH_ASSYMETRIC_KEYS = ImmutableMap.of("groups",
          ImmutableMap.of(MBeanSelector.QUERY_KEY, "groupName", MBeanSelector.INCLUDED_KEYS_KEY, "alpha|beta",
                "subgroup1", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name1", MBeanSelector.INCLUDED_KEYS_KEY, "abc.*", MBeanSelector.VALUES_KEY, "group1Val" ),
                "subgroup2", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name2", MBeanSelector.VALUES_KEY, "group2Val" )));

    @Test
    void whenNoKeysMatch_dontRequestMetrics() {
        MBeanSelector selector = MBeanSelector.create(DEEP_MAP_WITH_NONEXISTENT_KEYS);
        selector.offerKeys(DEEP_KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.groupName", containsInAnyOrder("alpha", "beta")));
        assertThat(selector.getRequest(), hasNoJsonPath("$.children.groups.children.subgroup1"));
        assertThat(selector.getRequest(), hasJsonPath("$.children.groups.children.subgroup2.name2", containsInAnyOrder("defabc", "def123", "def678")));
    }

    private static final Map<String, Object> DEEP_MAP_WITH_NONEXISTENT_KEYS = ImmutableMap.of("groups",
          ImmutableMap.of(MBeanSelector.QUERY_KEY, "groupName", MBeanSelector.INCLUDED_KEYS_KEY, "alpha|beta",
                "subgroup1", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name1", MBeanSelector.INCLUDED_KEYS_KEY, "qrs.*", MBeanSelector.VALUES_KEY, "group1Val" ),
                "subgroup2", ImmutableMap.of(MBeanSelector.QUERY_KEY, "name2", MBeanSelector.INCLUDED_KEYS_KEY, "def.*", MBeanSelector.VALUES_KEY, "group2Val" )));

    @Test
    void whenMapHasExcludedKeysAndSelectorLacksFoundKeys_needNewKeys() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_EXCLUDED_KEYS);

        assertThat(selector.needsNewKeys(), is(true));
    }

    private static final Map<String, Object> MAP_WITH_EXCLUDED_KEYS = ImmutableMap.of("servlets",
          ImmutableMap.of(MBeanSelector.QUERY_KEY, "servletName", MBeanSelector.EXCLUDED_KEYS_KEY, "alpha|beta"));

    @Test
    void afterKeysOffered_selectorHasAllButExcludedKeys() {
        MBeanSelector selector = MBeanSelector.create(MAP_WITH_EXCLUDED_KEYS);
        selector.offerKeys(KEY_RESPONSE);

        assertThat(selector.getRequest(), hasJsonPath("$.children.servlets.servletName", contains("gamma")));
    }

    @Test
    void whenMapLacksStringValues_selectorDoesNotAcceptThem() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));

        assertThat(selector.isStringMetric("first"), is(false));
    }

    @Test
    void whenMapHasStringValues_areIncludedInQueryFields() {
        MBeanSelector selector = createSelectorWithStringMetrics();

        assertThat(querySpec(selector), hasJsonPath("$.children.servlets.fields", containsInAnyOrder("first", "second", "state", "color")));
    }

    private MBeanSelector createSelectorWithStringMetrics() {
        return createSelectorWithStringMetrics(
            StringMetric.of("state", "open", "closed"),
            StringMetric.of("color", "red", "green"));
    }

    private MBeanSelector createSelectorWithStringMetrics(StringMetric... metrics) {
        return MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.VALUES_KEY, new String[] {"first", "second"},
                                MBeanSelector.STRING_VALUES_KEY, StringMetric.all(metrics))));
    }

    private static class StringMetric {
        private final String name;
        private final List<String> values;

        private static StringMetric of(String name, String... values) {
            return new StringMetric(name, values);
        }

        private static Map<String, List<String>> all(StringMetric... metrics) {
            final Map<String, List<String>> result = new HashMap<>();
            for (StringMetric metric : metrics)
                result.put(metric.name, metric.values);
            return result;
        }

        private StringMetric(String name, String[] values) {
            this.name = name;
            this.values = Arrays.asList(values);
        }
    }

    @Test
    void whenMapHasStringValues_recognizeStringMetrics() {
        MBeanSelector selector = createSelectorWithStringMetrics().getNestedSelectors().get("servlets");

        assertThat(selector.isStringMetric("first"), is(false));
        assertThat(selector.isStringMetric("second"), is(false));
        assertThat(selector.isStringMetric("state"), is(true));
        assertThat(selector.isStringMetric("color"), is(true));
    }

    @Test
    void whenMapHasStringValues_translateStringMetricsToIntegers() {
        MBeanSelector selector = createSelectorWithStringMetrics().getNestedSelectors().get("servlets");

        assertThat(selector.getStringMetricValue("first", "blank"), equalTo(-1));
        assertThat(selector.getStringMetricValue("state", "open"), equalTo(0));
        assertThat(selector.getStringMetricValue("state", "CLOSED"), equalTo(1));
        assertThat(selector.getStringMetricValue("color", "red"), equalTo(0));
        assertThat(selector.getStringMetricValue("color", "green"), equalTo(1));
        assertThat(selector.getStringMetricValue("color", "yellow"), equalTo(-1));
    }

    // todo catch bad type

    @Test
    void whenMergingLeafElements_combineValues() {
        MBeanSelector selector1 = createLeaf("first", "second");
        MBeanSelector selector2 = createLeaf("second", "third");

        assertThat(querySpec(selector1.merge(selector2)), hasJsonPath("$.fields", containsInAnyOrder("first", "second", "third")));
    }

    @Test
    void whenMergingLeafElements_combineStringValues() {
        MBeanSelector selector1 = createSelectorWithStringMetrics(StringMetric.of("state", "open", "closed"));
        MBeanSelector selector2 = createSelectorWithStringMetrics(StringMetric.of("color", "red", "green"));

        assertThat(querySpec(selector1.merge(selector2)),
              hasJsonPath("$.children.servlets.fields", containsInAnyOrder("first", "second", "state", "color")));
    }

    @Test
    void whenLeafElementsHaveMatchingAttributes_mayCombine() {
        MBeanSelector selector1 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers", "first", "second");
        MBeanSelector selector2 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers", "second", "third");

        assertThat(selector1.mayMergeWith(selector2), is(true));
    }

    @Test
    void whenLeafElementsHaveMatchingAttributes_mergedResultHasOriginalAttributes() {
        final String INCLUDED = MBeanSelector.USE_REGULAR_EXPRESSIONS ? "abc.*" : "abc*";
        final String EXCLUDED = MBeanSelector.USE_REGULAR_EXPRESSIONS ? "abcd.*" : "abcd*";
        MBeanSelector selector1 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers",
              "includedKeyValues:" + INCLUDED, "excludedKeyValues:" + EXCLUDED);
        MBeanSelector selector2 = createLeaf("type:Type1", "prefix:#_", "key:name", "keyName:numbers", "second", "third");

        MBeanSelector result = selector1.merge(selector2);
        assertThat(result.getType(), equalTo("Type1"));
        assertThat(result.getPrefix(), equalTo("#_"));
        assertThat(result.getKey(), equalTo("name"));
        assertThat(result.getKeyName(), equalTo("numbers"));
        assertThat(result.getIncludedKeys(), equalTo(INCLUDED));
        assertThat(result.getExcludedKeys(), equalTo(EXCLUDED));
    }

    @Test
    void whenLeafElementsHaveMisMatchedAttributes_mayNotCombine() {
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
            map.put(MBeanSelector.VALUES_KEY, Arrays.copyOfRange(params, i, params.length));
        return MBeanSelector.create(map);
    }

    @Test
    void whenSelectorsNoCommonNestedElementsWithSameName_mayMerge() {
        MBeanSelector selector1 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));
        MBeanSelector selector2 = MBeanSelector.create(ImmutableMap.of("kidlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));

        assertThat(selector1.mayMergeWith(selector2), is(true));
    }

    @Test
    void whenSelectorsHaveMismatchedNestedElementsWithSameName_mayNotMerge() {
        MBeanSelector selector1 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));
        MBeanSelector selector2 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));

        assertThat(selector1.mayMergeWith(selector2), is(false));
    }

    @Test
    void whenSelectorsHaveMismatchedNestedElementsWithDifferentName_merge() {
        MBeanSelector selector1 = MBeanSelector.create(ImmutableMap.of("servlets",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));
        MBeanSelector selector2 = MBeanSelector.create(ImmutableMap.of("ejbs",
                ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"})));
        MBeanSelector result = selector1.merge(selector2);

        assertThat(result.getNestedSelectors(), both(hasKey("servlets")).and(hasKey("ejbs")));
    }

    @Test
    void whenSelectorsHaveDeeplyNestedElementsWithDifferentName_mayMerge() {
        MBeanSelector selector1 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("servlets",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"}))));
        MBeanSelector selector2 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("ejbs",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"}))));

        assertThat(selector1.mayMergeWith(selector2), is(true));
    }

    @Test
    void whenSelectorsHaveDeeplyNestedElementsWithDifferentName_merge() {
        MBeanSelector selector1 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("servlets",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "oneKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"}))));
        MBeanSelector selector2 = MBeanSelector.create(
                ImmutableMap.of("components",
                    ImmutableMap.of("ejbs",
                        ImmutableMap.of(MBeanSelector.QUERY_KEY, "differentKey", MBeanSelector.VALUES_KEY, new String[] {"first", "second"}))));
        MBeanSelector result = selector1.merge(selector2);

        assertThat(result.getNestedSelectors(), aMapWithSize(1));
        assertThat(result.getNestedSelectors().get("components").getNestedSelectors(), both(hasKey("servlets")).and(hasKey("ejbs")));
    }

    @Test
    void generateJsonRequest() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("applicationRuntimes", getApplicationMap()));

        assertThat(parseString(selector.getRequest()), equalTo(parseString(compressedJsonForm(EXPECTED_JSON_REQUEST))));
    }

    private Map<String, Object> getApplicationMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name",
                               "componentRuntimes", getComponentMap());
    }

    private Map<String, Object> getComponentMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name", MBeanSelector.VALUES_KEY, EXPECTED_COMPONENT_VALUES,
                               "servlets", getServletMap());
    }

    @Test
    void domainNameSelector_selectsConfigurationUrl() {
        assertThat(MBeanSelector.DOMAIN_NAME_SELECTOR.getUrl(Protocol.HTTP, "myhost", 1234),
                   equalTo(String.format(QueryType.CONFIGURATION_URL_PATTERN, "http", "myhost", 1234)));
    }

    @Test
    void domainNameSelector_requestsName() {
        assertThat(MBeanSelector.DOMAIN_NAME_SELECTOR.getQueryValues(), arrayContaining("name"));
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
    void whenNoValuesListedForServlets_generateJsonRequest() {
        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("applicationRuntimes", getNoServletValuesApplicationMap()));

        assertThat(parseString(selector.getRequest()), equalTo(parseString(compressedJsonForm(EXPECTED_ALL_SERVLET_VALUES_JSON_REQUEST))));
    }

    private Map<String, Object> getNoServletValuesApplicationMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name",
                               "componentRuntimes", getNoServletValuesComponentMap());
    }

    private Map<String, Object> getNoServletValuesComponentMap() {
        return ImmutableMap.of(MBeanSelector.QUERY_KEY, "name", MBeanSelector.VALUES_KEY, EXPECTED_COMPONENT_VALUES,
                               "servlets", getNoValuesServletMap());
    }

    private Map<String, Serializable> getNoValuesServletMap() {
        return new HashMap<>(ImmutableMap.of(MBeanSelector.PREFIX_KEY, "servlet_", MBeanSelector.QUERY_KEY, EXPECTED_KEY));
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


    @Test
    void whenNoValuesListedForServletsAndStringValuesIncluded_generateJsonRequest() {
        final Map<String, Object> map = new HashMap<>(getNoServletValuesApplicationMap());
        final Map<String, Object> componentRuntimes = getSubMap(map, "componentRuntimes");
        final Map<String, Object> servlets = getSubMap(componentRuntimes, "servlets");
        servlets.put(MBeanSelector.STRING_VALUES_KEY, ImmutableMap.of("state", Arrays.asList("open", "closed")));

        MBeanSelector selector = MBeanSelector.create(ImmutableMap.of("applicationRuntimes", map));

        assertThat(parseString(selector.getRequest()), equalTo(parseString(compressedJsonForm(EXPECTED_ALL_SERVLET_VALUES_JSON_REQUEST))));
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> getSubMap(Map<String, Object> map, String key) {
        return (Map<String,Object>) map.get(key);
    }

    // todo - should we allow multiple types in a single filter?

}
