// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A description of an mbean to be selected by the generated JSON query and captured from the result.
 *
 * @author Russell Gold
 */
public class MBeanSelector {
    static final String TYPE_KEY = "type";
    static final String PREFIX_KEY = "prefix";
    static final String QUERY_KEY = "key";
    static final String KEY_NAME = "keyName";
    static final String VALUES_KEY = "values";
    static final String STRING_VALUES_KEY = "stringValues";
    static final String TYPE_FIELD_NAME = "type";
    static final MBeanSelector DOMAIN_NAME_SELECTOR = createDomainNameSelector();
    static final String NESTING = "  ";

    private String type;
    private String prefix;
    private String key;
    private String keyName;
    private List<String> values = new ArrayList<>();
    private Map<String, List<String>> stringValues;
    private Map<String, MBeanSelector> nestedSelectors = new LinkedHashMap<>();
    private QueryType queryType = QueryType.RUNTIME;

    private static MBeanSelector createDomainNameSelector() {
        Map<String,Object> yaml = new HashMap<>();
        yaml.put(MBeanSelector.VALUES_KEY, new String[] { "name" });
        MBeanSelector selector = MBeanSelector.create(yaml);
        selector.setQueryType(QueryType.CONFIGURATION);
        return selector;
    }

    private MBeanSelector(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case TYPE_KEY:
                    type = entry.getValue().toString();
                    break;
                case PREFIX_KEY:
                    prefix = entry.getValue().toString();
                    break;
                case QUERY_KEY:
                    this.key = entry.getValue().toString();
                    break;
                case KEY_NAME:
                    keyName = entry.getValue().toString();
                    break;
                case VALUES_KEY:
                    setValues(MapUtils.getStringArray(map, VALUES_KEY));
                    break;
                case STRING_VALUES_KEY:
                    addStringValues(entry.getValue());
                    break;
                default:
                    addNestedSelector(entry.getKey(), entry.getValue());
                    break;
            }
        }
    }

    private void addNestedSelector(String key, Object selectorValue) {
        try {
            nestedSelectors.put(key, createSelector(key, selectorValue));
        } catch (ConfigurationException e) {
            e.addContext(key);
            throw e;
        }
    }

    private void setValues(String[] values) {
        if (values.length == 0) throw new ConfigurationException("Values specified as empty array");
        
        Set<String> uniqueValues = new HashSet<>(Arrays.asList(values));
        if (values.length != uniqueValues.size())
            reportDuplicateValues(values, uniqueValues);
        this.values.addAll(Arrays.asList(values));
    }

    private void reportDuplicateValues(String[] values, Set<String> uniqueValues) {
        ArrayList<String> duplicate = new ArrayList<>(Arrays.asList(values));
        for (String unique : uniqueValues)
            duplicate.remove(unique);

        throw new ConfigurationException("Duplicate values for " + duplicate);
    }

    @SuppressWarnings("unchecked")
    private void addStringValues(Object value) {
        this.stringValues = (Map<String,List<String>>) value;
    }

    void appendNestedQuery(StringBuilder sb, String indent) {
        appendScalar(sb, indent, TYPE_KEY, type);
        appendScalar(sb, indent, PREFIX_KEY, prefix);
        appendScalar(sb, indent, QUERY_KEY, key);
        appendScalar(sb, indent, KEY_NAME, keyName);
        appendValues(sb, indent, values);
        appendStringValues(sb, indent, stringValues);

        for (String qualifier : getNestedSelectors().keySet()) {
            sb.append(indent).append(qualifier).append(":\n");
            getNestedSelectors().get(qualifier).appendNestedQuery(sb, indent + NESTING);
        }
    }

    private static void appendScalar(StringBuilder sb, String indent, String name, String value) {
        if (value != null)
            sb.append(indent).append(name).append(": ").append(value).append('\n');
    }

    private static void appendValues(StringBuilder sb, String indent, List<String> values) {
        if (values == null || values.size() == 0) return;
        if (values.size() == 1)
            appendScalar(sb, indent, VALUES_KEY, values.get(0));
        else
            appendArray(sb, indent, VALUES_KEY, values);
    }

    private static void appendArray(StringBuilder sb, String indent, String key, List<String> values) {
        sb.append(indent).append(key).append(": [").append(String.join(", ", values)).append("]\n");
    }

    private static void appendStringValues(StringBuilder sb, String indent, Map<String, List<String>> values) {
        if (values == null || values.size() == 0) return;
        sb.append(indent).append(STRING_VALUES_KEY).append(":\n");
        for (Map.Entry<String, List<String>> value : values.entrySet()) {
            appendArray(sb, indent + NESTING, value.getKey(), value.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private MBeanSelector createSelector(String key, Object value) {
        if (!(value instanceof Map))
            throw MapUtils.createBadTypeException(key, value, "a structure");

        return MBeanSelector.create((Map<String, Object>) value);
    }

    /**
     * Creates a hierarchical mbean selector based on a map derived from the YAML spec.
     * @param map the map of yaml data
     * @return a new mbean selector
     */
    public static MBeanSelector create(Map<String, Object> map) {
        return new MBeanSelector(map);
    }

    /**
     * Returns the type of mbean to process, from among those captured by this selector. If empty or null,
     * processes all captured mbeans.
     * @return a string matching the "type" attribute in the query result.
     */
    String getType() {
        return type;
    }

    /**
     * Returns the prefix to be appended to the names of any values extracted from the corresponding mbean.
     * @return a string. May be null.
     */
    String getPrefix() {
        return prefix;
    }

    /**
     * Returns the name of the field to use as a key to describe all values captured by this selector and its children.
     * The key-value pairs will be listed between braces after the value name.
     * @return the name of a field in the captured mbean.
     */
    String getKey() {
        return key;
    }

    /**
     * Returns the name to use to add a qualifier for nested metrics whose value is obtained from the
     * value whose name matches the key. If not specified, defaults to the underlying key name.
     * @return the qualifier name for the exported metric
     */
    String getKeyName() {
        return MapUtils.isNullOrEmptyString(keyName) ? key : keyName;
    }

    /**
     * Returns the names of fields in the underlying mbeans which should be exported.
     * @return an array of field names.
     */
    String[] getQueryValues() {
        final List<String> result = new ArrayList<>(values);
        if (stringValues != null) result.addAll(stringValues.keySet());

        return result.toArray(new String[0]);
    }

    private List<String> getValuesAsList() {
        return new ArrayList<>(values);
    }

    /**
     * Returns a map of nested mbean selectors
     * @return the nested selectors
     */
    Map<String, MBeanSelector> getNestedSelectors() {
        return nestedSelectors;
    }

    /**
     * Returns a JSON string query to be displayed.
     * @return a JSON string
     */
    public String getPrintableRequest() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(createQuerySpec());
    }

    /**
     * Returns a JSON string query to be sent to the REST service.
     * @return a JSON string
     */
    public String getRequest() {
        return new Gson().toJson(createQuerySpec());
    }

    private JsonQuerySpec createQuerySpec() {
        return toQuerySpec();
    }

    JsonQuerySpec toQuerySpec() {
        JsonQuerySpec spec = new JsonQuerySpec();
        if (!useAllValues())
            selectQueryFields(spec, getQueryValues());

        for (Map.Entry<String, MBeanSelector> selector : nestedSelectors.entrySet())
            spec.addChild(selector.getKey(), selector.getValue().toQuerySpec());

        return spec;
    }

    boolean useAllValues() {
        return prefix != null && values.isEmpty();
    }

    private void selectQueryFields(JsonQuerySpec spec, String[] fields) {
        if (key != null) spec.addFields(key);
        if (type != null) spec.addFields(TYPE_FIELD_NAME);
        spec.addFields(fields);
    }

    /**
     * Merges this selector with the specified one. Returns the result of the merge.
     * @param selector a new selector whose attributes are to be combined with this one
     * @return the combined selector
     */
    MBeanSelector merge(MBeanSelector selector) {
        return new MBeanSelector(this, selector);
    }

    private MBeanSelector(MBeanSelector first, MBeanSelector second) {
        this.type = first.type;
        this.prefix = first.prefix;
        this.key = first.key;
        this.keyName = first.keyName;

        final Set<String> mergedValues = new HashSet<>(first.getValuesAsList());
        mergedValues.addAll(second.getValuesAsList());
        values = new ArrayList<>(mergedValues);

        nestedSelectors = new LinkedHashMap<>();
        nestedSelectors.putAll(first.nestedSelectors);
        for (String k : second.nestedSelectors.keySet()) {
            if (!nestedSelectors.containsKey(k))
                nestedSelectors.put(k, second.nestedSelectors.get(k));
            else
                nestedSelectors.put(k, nestedSelectors.get(k).merge(second.nestedSelectors.get(k)));
        }
    }

    boolean mayMergeWith(MBeanSelector other) {
        if (!Objects.equals(keyName, other.keyName)) return false;
        if (!Objects.equals(key, other.key)) return false;
        if (!Objects.equals(type, other.type)) return false;
        if (!Objects.equals(prefix, other.prefix)) return false;

        for (String k : nestedSelectors.keySet())
            if (other.nestedSelectors.containsKey(k) && !mayMergeCorrespondingChildren(k, other)) return false;
        return true;
    }

    private boolean mayMergeCorrespondingChildren(String selectorKey, MBeanSelector other) {
        return nestedSelectors.get(selectorKey).mayMergeWith(other.nestedSelectors.get(selectorKey));
    }

    public String getUrl(Protocol protocol, String hostName, int port) {
        return protocol.format(queryType.getUrlPattern(), hostName, port);
    }

    public QueryType getQueryType() {
        return queryType;
    }

    @SuppressWarnings("SameParameterValue")
    void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    boolean acceptsStrings() {
        return queryType.acceptsStrings();
    }

    boolean isStringMetric(String fieldName) {
        return stringValues != null && stringValues.containsKey(fieldName);
    }

    int getStringMetricValue(String fieldName, String value) {
        if (!isStringMetric(fieldName))
            return -1;
        else {
            return getIndex(value, stringValues.get(fieldName));
        }
    }

    private int getIndex(String value, List<String> fieldValues) {
        for (int i = 0; i < fieldValues.size(); i++) {
            if (value.equalsIgnoreCase(fieldValues.get(i))) {
                return i;
            }
        }
        return -1;
    }

    void processMetrics(Map<String, Object> metrics, Consumer<Map<String, String>> processMetrics) {
        queryType.processMetrics(metrics, processMetrics);
    }
}
