// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import static com.oracle.wls.exporter.domain.MapUtils.isNullOrEmptyString;

/**
 * A class which can scrape metrics from a JSON REST response.
 *
 * @author Russell Gold
 */
class MetricsScraper {
    private static final char QUOTE = '"';
    private final String globalQualifiers;
    private Map<String, Object> metrics = new HashMap<>();
    private boolean metricNameSnakeCase;

    MetricsScraper(String globalQualifiers) {
        this.globalQualifiers = globalQualifiers;
    }

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
        scrapeItem(response, selector, globalQualifiers);
        return metrics;
    }

    void setMetricNameSnakeCase(boolean metricNameSnakeCase) {
        this.metricNameSnakeCase = metricNameSnakeCase;
    }

    private void scrapeItemList(JsonObject itemWrapper, MBeanSelector beanSelector, String parentQualifiers) {
        JsonArray items = itemWrapper.getAsJsonArray("items");
        if (items == null)
            scrapeItem(itemWrapper, beanSelector, parentQualifiers);
        else
            for (JsonElement jsonElement : items)
                scrapeItem(jsonElement.getAsJsonObject(), beanSelector, parentQualifiers);
    }

    private void scrapeItem(JsonObject object, MBeanSelector beanSelector, String parentQualifiers) {
        if (excludeByType(object.get("type"), beanSelector.getType())) return;
        String qualifiers = getItemQualifiers(object, beanSelector, parentQualifiers);

        for (String valueName : getValueNames(beanSelector, object)) {
            if (valueName.equals(beanSelector.getKey())) continue;
            JsonElement value = object.get(valueName);
            addMetric(beanSelector, qualifiers, valueName, value);
        }
        scrapeSubObject(object, beanSelector, qualifiers);
    }

    private String[] getValueNames(MBeanSelector beanSelector, JsonObject object) {
        final Set<String> set = object.keySet();
        return beanSelector.useAllValues() ? asArray(set) : beanSelector.getValues();
    }

    private String[] asArray(Set<String> set) {
        return set.toArray(new String[0]);
    }

    private void addMetric(MBeanSelector beanSelector, String qualifiers, String valueName, JsonElement value) {
        if (value != null && value.isJsonPrimitive())
            addMetric(beanSelector, qualifiers, valueName, value.getAsJsonPrimitive());
    }

    private void addMetric(MBeanSelector beanSelector, String qualifiers, String valueName, JsonPrimitive jsonPrimitive) {
        if (jsonPrimitive.isNumber())
            metrics.put(getMetricName(valueName, beanSelector, qualifiers), jsonPrimitive.getAsNumber());
        else if (beanSelector.acceptsStrings() && jsonPrimitive.isString())
            metrics.put(getMetricName(valueName, beanSelector, qualifiers), jsonPrimitive.getAsString());
    }

    private boolean excludeByType(JsonElement typeField, String typeFilter) {
        return typeFilter != null && typeField != null && !typeFilter.equals(typeField.getAsString());
    }

    void scrapeSubObject(JsonObject subObject, MBeanSelector selector, String parentQualifiers) {
        for (String selectorKey : selector.getNestedSelectors().keySet()) {
            JsonElement value = subObject.get(selectorKey);
            if (value instanceof JsonObject) {
                scrapeItemList(((JsonObject) value), selector.getNestedSelectors().get(selectorKey), parentQualifiers);
            }
        }
    }

    private String getMetricName(String valueName, MBeanSelector beanSelector, String qualifiers) {
        StringBuilder sb = new StringBuilder();
        if (beanSelector.getPrefix() != null) sb.append(withCorrectCase(beanSelector.getPrefix()));
        sb.append(withCorrectCase(valueName));
        if (!isNullOrEmptyString(qualifiers))
            sb.append('{').append(qualifiers).append('}');
        return sb.toString();
    }

    private String withCorrectCase(String valueName) {
        return metricNameSnakeCase ? SnakeCaseUtil.convert(valueName) : valueName;
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
