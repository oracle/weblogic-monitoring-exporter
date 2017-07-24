package io.prometheus.wls.rest.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/**
 * A class which can scrape metrics from a JSON REST response.
 */
public class MetricsScraper {
    private static final char QUOTE = '"';
    private Map<String, Object> metrics = new HashMap<>();

    /**
     * Returns the metrics from the last scrape.
     * @return a map of metric name to value
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }

    /**
     * Scrapes metrics from a response, in accordance with the rules defined in the selector.
     * @param selector an mbean selector, configured with the metrics we want to find
     * @param response a parsed JSON REST response
     */
    public void scrape(MBeanSelector selector, JsonObject response) {
        clearMetrics();
        if (selector.getNestedSelectors().containsKey(MBeanSelector.PARENT_RUNTIME_LIST))
            scrapeItem(response, selector, "");
        else
            scrapeItemList(response.get(MBeanSelector.PARENT_RUNTIME_LIST).getAsJsonObject(), selector, "");
    }

    private void clearMetrics() {
        metrics = new HashMap<>();
    }

    private void scrapeItemList(JsonObject itemWrapper, MBeanSelector beanSelector, String parentQualifiers) {
        JsonArray items = itemWrapper.getAsJsonArray("items");
        for (JsonElement jsonElement : items)
            scrapeItem(jsonElement.getAsJsonObject(), beanSelector, parentQualifiers);
    }

    private void scrapeItem(JsonObject object, MBeanSelector beanSelector, String parentQualifiers) {
        if (excludeByType(object.get("type"), beanSelector.getType())) return;
        String qualifiers = getItemQualifiers(object, beanSelector, parentQualifiers);

        for (String valueName : beanSelector.getValues()) {
            JsonElement value = object.get(valueName);
            if (value != null && value.isJsonPrimitive())
                metrics.put(getMetricName(valueName, beanSelector, qualifiers), getPrimitiveValue(value.getAsJsonPrimitive()));
        }
        scrapeSubObject(object, beanSelector, qualifiers);
    }

    private boolean excludeByType(JsonElement typeField, String typeFilter) {
        return typeFilter != null && typeField != null && !typeFilter.equals(typeField.getAsString());
    }

    void scrapeSubObject(JsonObject subObject, MBeanSelector selector, String parentQualifiers) {
        for (String selectorKey : selector.getNestedSelectors().keySet()) {
            if (subObject.has(selectorKey)) {
                scrapeItemList(subObject.getAsJsonObject(selectorKey), selector.getNestedSelectors().get(selectorKey), parentQualifiers);
            }
        }
    }

    private String getMetricName(String valueName, MBeanSelector beanSelector, String qualifiers) {
        StringBuilder sb = new StringBuilder(beanSelector.getPrefix()).append(valueName);
        if (MapUtils.isNotNullOrEmptyString(qualifiers))
            sb.append('{').append(qualifiers).append('}');
        return sb.toString();
    }

    private Object getPrimitiveValue(JsonPrimitive value) {
        if (value.isNumber())
            return value.getAsNumber();
        else
            return value.getAsString();
    }

    private String getItemQualifiers(JsonObject object, MBeanSelector beanSelector, String parentQualifiers) {
        String qualifiers = parentQualifiers;
        if (object.has(beanSelector.getKey())) {
            if (MapUtils.isNotNullOrEmptyString(qualifiers)) qualifiers += ',';
            qualifiers += (beanSelector.getKeyName() + '=' + asQuotedString(object.get(beanSelector.getKey())));
        }
        return qualifiers;
    }

    private String asQuotedString(JsonElement jsonElement) {
        return QUOTE + jsonElement.getAsString() + QUOTE;
    }
}
