package io.prometheus.wls.rest.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

/**
 * A class which can scrape metrics from a JSON REST response.
 */
class MetricsScraper {
    private static final char QUOTE = '"';
    private Map<String, Object> metrics = new HashMap<>();

    Map<String, Object> getMetrics() {
        return metrics;
    }

    /**
     * Scrapes metrics from a response, in accordance with the rules defined in the selector.
     * @param selector an mbean selector, configured with the metrics we want to find
     * @param response a parsed JSON REST response
     */
    Map<String, Object> scrape(MBeanSelector selector, JsonObject response) {
        metrics = new HashMap<>();
        scrapeItem(response, selector, "");
        return metrics;
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
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber())
                metrics.put(getMetricName(valueName, beanSelector, qualifiers), value.getAsJsonPrimitive().getAsNumber());
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
        StringBuilder sb = new StringBuilder();
        if (beanSelector.getPrefix() != null) sb.append(beanSelector.getPrefix());
        sb.append(valueName);
        if (!isNullOrEmptyString(qualifiers))
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
            if (!isNullOrEmptyString(qualifiers)) qualifiers += ',';
            qualifiers += (beanSelector.getKeyName() + '=' + asQuotedString(object.get(beanSelector.getKey())));
        }
        return qualifiers;
    }

    private String asQuotedString(JsonElement jsonElement) {
        return QUOTE + jsonElement.getAsString() + QUOTE;
    }
}
