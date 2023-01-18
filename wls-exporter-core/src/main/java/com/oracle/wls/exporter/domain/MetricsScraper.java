// Copyright (c) 2017, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            final String itemQualifiers = getItemQualifiers();

            for (String valueName : getValueNames()) {
                scrapeValue(itemQualifiers, valueName);
            }

            scrapeSubObjects(itemQualifiers);
        }

        private void scrapeValue(String itemQualifiers, String valueName) {
            if (valueName.equals(selector.getKey())) return;

            new ScrapedMetric(itemQualifiers, valueName).add();
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


        class ScrapedMetric {
            private final String itemQualifiers;
            private final String valueName;
            private final JsonPrimitive jsonPrimitive;

            ScrapedMetric(String itemQualifiers, String valueName) {
                this.itemQualifiers = itemQualifiers;
                this.valueName = valueName;

                this.jsonPrimitive = Optional.ofNullable(object.get(valueName))
                      .filter(JsonElement::isJsonPrimitive)
                      .map(JsonElement::getAsJsonPrimitive)
                      .orElse(null);
            }

            void add() {
                Optional.ofNullable(jsonPrimitive).map(this::toMetricValue).ifPresent(v -> metrics.put(getMetricName(), v));
            }

            private Object toMetricValue(JsonPrimitive jsonPrimitive) {
                if (jsonPrimitive.isNumber())
                    return jsonPrimitive.getAsNumber();
                else if (isStringMetric())
                    return selector.getStringMetricValue(valueName, jsonPrimitive.getAsString());
                else if (selector.acceptsStrings() && jsonPrimitive.isString())
                    return jsonPrimitive.getAsString();
                else
                    return null;
            }

            private boolean isStringMetric() {
                return selector.isStringMetric(valueName) && jsonPrimitive.isString();
            }

            private String getMetricName() {
                StringBuilder sb = new StringBuilder();
                if (selector.getPrefix() != null) sb.append(getCorrectCase(selector.getPrefix()));
                sb.append(getCorrectCase(valueName));
                if (!isNullOrEmptyString(itemQualifiers))
                    sb.append('{').append(augmented(itemQualifiers)).append('}');
                return sb.toString();
            }

            private String getCorrectCase(String valueName) {
                return metricNameSnakeCase ? SnakeCaseUtil.convert(valueName) : valueName;
            }

            private String augmented(String itemQualifiers) {
                if (isStringMetric())
                    return itemQualifiers + ",value=\"" + jsonPrimitive.getAsString() + '"';
                else
                    return itemQualifiers;
            }
        }
    }

}
