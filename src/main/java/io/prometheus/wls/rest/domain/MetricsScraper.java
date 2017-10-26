package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

/**
 * A class which can scrape metrics from a JSON REST response.
 *
 * @author Russell Gold
 */
class MetricsScraper {
    private static final char QUOTE = '"';
    private Map<String, Object> metrics = new HashMap<>();
    private boolean metricNameSnakeCase;

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
        return set.toArray(new String[set.size()]);
    }

    private void addMetric(MBeanSelector beanSelector, String qualifiers, String valueName, JsonElement value) {
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber())
            metrics.put(getMetricName(valueName, beanSelector, qualifiers), value.getAsJsonPrimitive().getAsNumber());
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
