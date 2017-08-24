package io.prometheus.wls.rest.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A description of an mbean to be selected by the generated JSON query and captured from the result.
 */
public class MBeanSelector {
    static final String TYPE = "type";
    static final String PREFIX = "prefix";
    static final String KEY = "key";
    static final String KEY_NAME = "keyName";
    static final String VALUES = "values";
    static final String TYPE_FIELD_NAME = "type";

    private String type;
    private String prefix;
    private String key;
    private String keyName;
    private String[] values = {};
    private Map<String, MBeanSelector> nestedSelectors = new HashMap<>();

    private MBeanSelector(Map<String, Object> map) {
        for (String key : map.keySet()) {
            switch (key) {
                case TYPE:
                    type = MapUtils.getStringValue(map, TYPE);
                    break;
                case PREFIX:
                    prefix = MapUtils.getStringValue(map, PREFIX);
                    break;
                case KEY:
                    this.key = MapUtils.getStringValue(map, KEY);
                    break;
                case KEY_NAME:
                    keyName = MapUtils.getStringValue(map, KEY_NAME);
                    break;
                case VALUES:
                    setValues(MapUtils.getStringArray(map, VALUES));
                    break;
                default:
                    addNestedSelector(key, map.get(key));
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
        this.values = values;
    }

    private void reportDuplicateValues(String[] values, Set<String> uniqueValues) {
        ArrayList<String> duplicate = new ArrayList<>(Arrays.asList(values));
        for (String unique : uniqueValues)
            duplicate.remove(duplicate.indexOf(unique));

        throw new ConfigurationException("Duplicate values for " + duplicate);
    }

    void appendNestedQuery(StringBuilder sb, String indent) {
        appendScalar(sb, indent, "type", type);
        appendScalar(sb, indent, "prefix", prefix);
        appendScalar(sb, indent, "key", key);
        appendScalar(sb, indent, "keyName", keyName);
        appendValues(sb, indent, values);

        for (String qualifier : getNestedSelectors().keySet()) {
            sb.append(indent).append(qualifier).append(":\n");
            getNestedSelectors().get(qualifier).appendNestedQuery(sb, indent + "  ");
        }
    }

    private static void appendScalar(StringBuilder sb, String indent, String name, String value) {
        if (value != null)
            sb.append(indent).append(name).append(": ").append(value).append('\n');
    }

    private static void appendValues(StringBuilder sb, String indent, String[] values) {
        if (values == null || values.length == 0) return;
        if (values.length == 1)
            appendScalar(sb, indent, "values", values[0]);
        else
            sb.append(indent).append("values").append(": [").append(String.join(", ", values)).append("]\n");
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
    static MBeanSelector create(Map<String, Object> map) {
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
    String[] getValues() {
        return values;
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
        if (key != null) spec.addFields(key);
        if (type != null) spec.addFields(TYPE_FIELD_NAME);
        spec.addFields(values);

        for (String selectorName : nestedSelectors.keySet())
            spec.addChild(selectorName, nestedSelectors.get(selectorName).toQuerySpec());

        return spec;
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
        type = first.type;
        prefix = first.prefix;
        key = first.key;
        keyName = first.keyName;

        Set<String> mergedValues = new HashSet<>(Arrays.asList(first.values));
        mergedValues.addAll(Arrays.asList(second.values));
        values = mergedValues.toArray(new String[mergedValues.size()]);

        nestedSelectors = new HashMap<>();
        nestedSelectors.putAll(first.nestedSelectors);
        for (String key : second.nestedSelectors.keySet()) {
            if (!nestedSelectors.containsKey(key))
                nestedSelectors.put(key, second.nestedSelectors.get(key));
            else
                nestedSelectors.put(key, nestedSelectors.get(key).merge(second.nestedSelectors.get(key)));
        }
    }

    boolean mayMergeWith(MBeanSelector other) {
        if (!Objects.equals(keyName, other.keyName)) return false;
        if (!Objects.equals(key, other.key)) return false;
        if (!Objects.equals(type, other.type)) return false;
        if (!Objects.equals(prefix, other.prefix)) return false;

        for (String key : nestedSelectors.keySet())
            if (other.nestedSelectors.containsKey(key) && !mayMergeCorrespondingChildren(key, other)) return false;
        return true;
    }

    private boolean mayMergeCorrespondingChildren(String selectorKey, MBeanSelector other) {
        return nestedSelectors.get(selectorKey).mayMergeWith(other.nestedSelectors.get(selectorKey));
    }
}
