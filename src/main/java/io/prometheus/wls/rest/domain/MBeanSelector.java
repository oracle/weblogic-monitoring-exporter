package io.prometheus.wls.rest.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

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
                    values = MapUtils.getStringArray(map, VALUES);
                    break;
                default:
                    nestedSelectors.put(key, createSelector(key, map.get(key)));
                    break;
            }
        }
    }

    /**
     * Creates a set of metrics from a Json object
     * @param response a parsed JSON REST response
     */
    public Map<String, Object> scrapeMetrics(JsonObject response) {
        return new MetricsScraper().scrape(this, response);
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

}
