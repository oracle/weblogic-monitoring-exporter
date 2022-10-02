// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    void setMetricNameSnakeCase(boolean metricNameSnakeCase) {
        this.metricNameSnakeCase = metricNameSnakeCase;
    }

    /**
     * Scrapes metrics from a response, in accordance with the rules defined in the selector.
     * @param selector an mbean selector, configured with the metrics we want to find
     * @param response a parsed JSON REST response
     */
    Map<String, Object> scrape(MBeanSelector selector, JsonObject response) {
        metrics = new HashMap<>();
        createDelegate(selector, response).scrapeItem();
        return metrics;
    }

    ScrapeDelegate createDelegate(MBeanSelector selector, JsonObject response) {
        return new ScrapeDelegate(selector, response, globalQualifiers);
    }

    class ScrapeDelegate {
        private final MBeanSelector selector;
        private final JsonObject object;
        private final String qualifiers;

        ScrapeDelegate(MBeanSelector selector, JsonObject object, String qualifiers) {
            this.selector = selector;
            this.object = object;
            this.qualifiers = qualifiers;
        }

        void scrapeSubObjects(String qualifiers) {
            for (String selectorKey : selector.getNestedSelectors().keySet()) {
                final JsonElement value = object.get(selectorKey);
                if (value instanceof JsonObject) {
                    final MBeanSelector nestedSelector = selector.getNestedSelectors().get(selectorKey);
                    createForSubObjects(nestedSelector, (JsonObject) value, qualifiers).scrapeItemOrList();
                }
            }
        }

        private ScrapeDelegate createForSubObjects(MBeanSelector selector, JsonObject value, String qualifiers) {
            return new ScrapeDelegate(selector, value, qualifiers);
        }

        private void scrapeItemOrList() {
            JsonArray items = object.getAsJsonArray("items");
            if (items == null)
                scrapeItem();
            else
                for (JsonElement jsonElement : items)
                    createForItem(jsonElement.getAsJsonObject()).scrapeItem();
        }

        private ScrapeDelegate createForItem(JsonObject asJsonObject) {
            return new ScrapeDelegate(selector, asJsonObject, qualifiers);
        }

        private String[] getValueNames() {
            return selector.useAllValues() ? getKeysAsArray() : selector.getQueryValues();
        }

        private String[] getKeysAsArray() {
            return object.keySet().toArray(new String[0]);
        }

        private void scrapeItem() {
            if (excludeByType()) return;
            String itemQualifiers = getItemQualifiers();

            for (String valueName : getValueNames()) {
                if (valueName.equals(selector.getKey())) continue;
                JsonElement value = object.get(valueName);
                addMetric(itemQualifiers, valueName, value);
            }
            scrapeSubObjects(itemQualifiers);
        }

        private boolean excludeByType() {
            final JsonElement typeField = object.get("type");
            final String typeFilter = selector.getType();
            return typeFilter != null && typeField != null && !typeFilter.equals(typeField.getAsString());
        }

        private String getItemQualifiers() {
            String result = qualifiers;
            if (object.has(selector.getKey())) {
                if (!isNullOrEmptyString(result)) result += ',';
                result += selector.getKeyName() + uniqueSuffix() +'=' + asQuotedString(object.get(selector.getKey()));
            }
            return result;
        }

        private Object uniqueSuffix() {
            return qualifiers.startsWith(selector.getKeyName() + '=') ? '2' : "";
        }

        private String asQuotedString(JsonElement jsonElement) {
            return QUOTE + jsonElement.getAsString() + QUOTE;
        }

        private void addMetric(String itemQualifiers, String valueName, JsonElement value) {
            if (value != null && value.isJsonPrimitive()) {
                addMetric(itemQualifiers, valueName, value.getAsJsonPrimitive());
            }
        }

        private void addMetric(String itemQualifiers, String valueName, JsonPrimitive jsonPrimitive) {
            Optional.ofNullable(getMetricValue(valueName, jsonPrimitive))
                .ifPresent(value -> addMetric(getMetricName(itemQualifiers, valueName), value));
        }

        private void addMetric(String metricName, Object value) {
            metrics.put(metricName, value);
        }

        private Object getMetricValue(String valueName, JsonPrimitive jsonPrimitive) {
            if (jsonPrimitive.isNumber())
                return jsonPrimitive.getAsNumber();
            else if (selector.isStringMetric(valueName) && jsonPrimitive.isString())
                return selector.getStringMetricValue(valueName, jsonPrimitive.getAsString());
            else if (selector.acceptsStrings() && jsonPrimitive.isString())
                return jsonPrimitive.getAsString();
            else
                return null;
        }

        private String getMetricName(String itemQualifiers, String valueName) {
            StringBuilder sb = new StringBuilder();
            if (selector.getPrefix() != null) sb.append(getCorrectCase(selector.getPrefix()));
            sb.append(getCorrectCase(valueName));
            if (!isNullOrEmptyString(itemQualifiers))
                sb.append('{').append(itemQualifiers).append('}');
            return sb.toString();
        }

        private String getCorrectCase(String valueName) {
            return metricNameSnakeCase ? SnakeCaseUtil.convert(valueName) : valueName;
        }
    }

}
