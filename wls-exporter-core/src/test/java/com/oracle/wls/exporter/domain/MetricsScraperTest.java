// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static com.oracle.wls.exporter.domain.MetricMatcher.hasMetric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.not;

/**
 * @author Russell Gold
 */
public class MetricsScraperTest {
    private static final String RESPONSE =
            "{\"applicationRuntimes\": {\"items\": [\n" +
            "     {\n" +
            "            \"internal\": false,\n" +
            "            \"name\": \"mbeans\",\n" +
            "            \"componentRuntimes\": {\"items\": [{\n" +
            "                \"deploymentState\": 2,\n" +
            "                \"name\": \"EjbStatusBean\",\n" +
            "                \"type\": \"EJBComponentRuntime\"\n" +
            "              }]}\n" +
            "     },\n" +
            "     {\n" +
            "            \"internal\": true,\n" +
            "            \"name\": \"weblogic\",\n" +
            "            \"componentRuntimes\": {\"items\": [{\n" +
            "                \"type\": \"WebAppComponentRuntime\",\n" +
            "                \"sourceInfo\": \"weblogic.war\",\n" +
            "                \"contextRoot\": \"\\/weblogic\",\n" +
            "                \"openSessionsCurrentCount\": 0,\n" +
            "                \"deploymentState\": 2,\n" +
            "                \"sessionsOpenedTotalCount\": 0,\n" +
            "                \"name\": \"ejb30_weblogic\",\n" +
            "                \"openSessionsHighCount\": 0,\n" +
            "                \"servlets\": {\"items\": [\n" +
            "                    {\n" +
            "                        \"servletName\": \"JspServlet\",\n" +
            "                        \"invocationTotalCount\": 0\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"FileServlet\",\n" +
            "                        \"invocationTotalCount\": 0\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"ready\",\n" +
            "                        \"invocationTotalCount\": 2\n" +
            "                    }\n" +
            "                  ]}\n" +
            "            }]}\n" +
            "     }\n" +
            "]}}";

    private static final String TWO_LEVEL_RESPONSE ="{\n" +
            "            \"internal\": true,\n" +
            "            \"name\": \"weblogic\",\n" +
            "            \"componentRuntimes\": {\"items\": [{\n" +
            "                \"type\": \"WebAppComponentRuntime\",\n" +
            "                \"sourceInfo\": \"weblogic.war\",\n" +
            "                \"internal\": true,\n" +
            "                \"openSessionsCurrentCount\": 0,\n" +
            "                \"deploymentState\": 2,\n" +
            "                \"sessionsOpenedTotalCount\": 0,\n" +
            "                \"name\": \"ejb30_weblogic\",\n" +
            "                \"openSessionsHighCount\": 0,\n" +
            "                \"servlets\": {\"items\": [\n" +
            "                    {\n" +
            "                        \"servletName\": \"JspServlet\",\n" +
            "                        \"invocationTotalCount\": 0\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"FileServlet\",\n" +
            "                        \"invocationTotalCount\": 0\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"ready\",\n" +
            "                        \"invocationTotalCount\": 2\n" +
            "                    }\n" +
            "                ]}\n" +
            "            }]}\n" +
            "        }";

    private static final String SERVLET_RESPONSE = "{\n" +
            "                \"servlets\": {\"items\": [\n" +
            "                    {\n" +
            "                        \"servletName\": \"JspServlet\",\n" +
            "                        \"invocationHighCount\": 10,\n" +
            "                        \"invocationTotalCount\": 0\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"FileServlet\",\n" +
            "                        \"invocationHighCount\": 15,\n" +
            "                        \"invocationTotalCount\": 1\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"ready\",\n" +
            "                        \"invocationHighCount\": 30,\n" +
            "                        \"invocationTotalCount\": 2\n" +
            "                    }\n" +
            "                ]}\n" +
            "            }";

    private static final String SINGLE_SERVLET_RESPONSE = "{\n" +
            "                \"servlets\": {\"items\": [\n" +
            "                    {\n" +
            "                        \"servletName\": \"JspServlet\",\n" +
            "                        \"invocationId\": 23,\n" +
            "                        \"invocationTotalCount\": 0\n" +
            "                    }\n" +
            "                ]}\n" +
            "            }";

    private static final String RESPONSE_WITH_NULL = "{\n" +
            "                \"servlets\": null\n" +
            "            }";

    private static final String CONFIG_RESPONSE = "{\"name\": \"mydomain\"}";

    private final Map<String,Object> leafMap = new HashMap<>(ImmutableMap.of(MBeanSelector.KEY, "servletName",
            MBeanSelector.PREFIX, "servlet_", MBeanSelector.VALUES, new String[]{"invocationTotalCount","invocationId"}));

    private final Map<String,Object> emptyLeafMap = new HashMap<>(ImmutableMap.of(MBeanSelector.KEY, "servletName",
            MBeanSelector.PREFIX, "servlet_"));

    private final Map<String,Object> componentMap = new HashMap<>(
            ImmutableMap.of(MBeanSelector.KEY_NAME, "component", MBeanSelector.KEY, "name", MBeanSelector.PREFIX, "component_",
                            "servlets", leafMap,
                            MBeanSelector.VALUES, new String[]{"sourceInfo", "internal", "deploymentState"}));

    private final Map<String,Object> noPrefixComponentMap = new HashMap<>(
            ImmutableMap.of(MBeanSelector.KEY_NAME, "component", MBeanSelector.KEY, "name",
                            "servlets", leafMap,
                            MBeanSelector.VALUES, new String[]{"sourceInfo", "internal", "deploymentState"}));

    private final Map<String,Object> twoLevelMap = ImmutableMap.of("componentRuntimes", componentMap);

    private final Map<String,Object> noPrefixTwoLevelMap = ImmutableMap.of("componentRuntimes", noPrefixComponentMap);

    private final MetricsScraper scraper = new MetricsScraper("");

    @Test
    public void generateLeafMetrics() {
        generateNestedMetrics(getServletsMap(), SERVLET_RESPONSE);

        assertThat(scraper.getMetrics(),
                   allOf(hasMetric("servlet_invocationTotalCount{servletName=\"JspServlet\"}", 0),
                         hasMetric("servlet_invocationTotalCount{servletName=\"FileServlet\"}", 1),
                         hasMetric("servlet_invocationTotalCount{servletName=\"ready\"}", 2)));
    }

    private ImmutableMap<String, Object> getServletsMap() {
        return ImmutableMap.of("servlets", leafMap);
    }

    private void generateNestedMetrics(Map<String,Object> map, String jsonString) {
        generateNestedMetrics(map, jsonString, "");
    }

    private void generateNestedMetrics(Map<String,Object> map, String jsonString, String parentQualifiers) {
        scraper.scrapeSubObject(getJsonResponse(jsonString), MBeanSelector.create(map), parentQualifiers);
    }

    private JsonObject getJsonResponse(String jsonString) {
        return new JsonParser().parse(jsonString).getAsJsonObject();
    }

    @Test
    public void whenNoValuesSpecified_generateAllMetrics() {
        generateNestedMetrics(getAllValuesServletsMap(), SERVLET_RESPONSE);

        assertThat(scraper.getMetrics(),
                   allOf(hasMetric("servlet_invocationHighCount{servletName=\"JspServlet\"}", 10),
                         hasMetric("servlet_invocationHighCount{servletName=\"FileServlet\"}", 15),
                         hasMetric("servlet_invocationTotalCount{servletName=\"ready\"}", 2)));
    }

    private ImmutableMap<String, Object> getAllValuesServletsMap() {
        return ImmutableMap.of("servlets", emptyLeafMap);
    }

    @Test
    public void generateLeafMetricsWhileAccumulatingQualifiers() {
        generateNestedMetrics(getServletsMap(), SERVLET_RESPONSE, "webapp=\"wls\"");

        assertThat(scraper.getMetrics(),
                   hasMetric("servlet_invocationTotalCount{webapp=\"wls\",servletName=\"JspServlet\"}", 0));
    }

    @Test
    public void generateLeafMetricsWithNoQualifiers() {
        generateNestedMetrics(getServletsMapWithoutQualifierKey(), SINGLE_SERVLET_RESPONSE);

        assertThat(scraper.getMetrics(), hasMetric("servlet_invocationTotalCount", 0));
    }

    private ImmutableMap<String, Object> getServletsMapWithoutQualifierKey() {
        leafMap.remove(MBeanSelector.KEY);
        return ImmutableMap.of("servlets", leafMap);
    }

    @Test
    public void generateLeafMetricsWithParentQualifiersOnly() {
        generateNestedMetrics(getServletsMapWithoutQualifierKey(), SINGLE_SERVLET_RESPONSE, "webapp=\"wls\"");

        assertThat(scraper.getMetrics(), hasMetric("servlet_invocationTotalCount{webapp=\"wls\"}", 0));
    }

    @Test
    public void whenKeyNameSpecified_useItRatherThanKey() {
        generateNestedMetrics(getServletsMapWithKeyName("servlet"), SERVLET_RESPONSE);

        assertThat(scraper.getMetrics(), hasMetric("servlet_invocationTotalCount{servlet=\"JspServlet\"}", 0));
    }

    @SuppressWarnings("SameParameterValue")
    private Map<String, Object> getServletsMapWithKeyName(String keyName) {
        leafMap.put(MBeanSelector.KEY_NAME, keyName);
        return ImmutableMap.of("servlets", leafMap);
    }

    @Test
    public void whenValuesIncludesKey_ignoreIt() {
        generateNestedMetrics(getServletsMapWithKey("invocationId"), SINGLE_SERVLET_RESPONSE);

        assertThat(scraper.getMetrics(), not(hasMetric("servlet_invocationId{invocationId=\"23\"}", 23)));
    }

    @SuppressWarnings("SameParameterValue")
    private Map<String, Object> getServletsMapWithKey(String key) {
        leafMap.put(MBeanSelector.KEY, key);
        return ImmutableMap.of("servlets", leafMap);
    }

    @Test
    public void whenNullObjectInResponse_dontFail() {
        generateNestedMetrics(getServletsMapWithKey("invocationId"), RESPONSE_WITH_NULL);

        // todo add as comment
        assertThat(scraper.getMetrics(), not(hasMetric("servlet_invocationId{invocationId=\"23\"}", 23)));
    }

    @Test
    public void whenGenerateHierarchicalMetrics_containsTopLevel() {
        generateNestedMetrics(twoLevelMap, TWO_LEVEL_RESPONSE);

        assertThat(scraper.getMetrics(), hasMetric("component_deploymentState{component=\"ejb30_weblogic\"}", 2));
    }

    @Test
    public void whenNoPrefix_generateBareMetrics() {
        generateNestedMetrics(noPrefixTwoLevelMap, TWO_LEVEL_RESPONSE);

        assertThat(scraper.getMetrics(), hasMetric("deploymentState{component=\"ejb30_weblogic\"}", 2));
    }

    @Test
    public void whenGenerateHierarchicalMetrics_ignoresNonNumericValues() {
        generateNestedMetrics(twoLevelMap, TWO_LEVEL_RESPONSE);

        assertThat(scraper.getMetrics(), not(hasMetric("component_sourceInfo{component=\"ejb30_weblogic\"}", "weblogic.war")));
        assertThat(scraper.getMetrics(), not(hasMetric("component_internal{component=\"ejb30_weblogic\"}", "true")));
    }

    @Test
    public void configurationQuery_acceptsStringValues() {
        scraper.scrape(MBeanSelector.DOMAIN_NAME_SELECTOR, getJsonResponse(CONFIG_RESPONSE));

        assertThat(scraper.getMetrics(), hasMetric("name", "mydomain"));
    }

    @Test
    public void whenGenerateHierarchicalMetrics_containsBottomLevel() {
        generateNestedMetrics(twoLevelMap, TWO_LEVEL_RESPONSE);

        assertThat(scraper.getMetrics(), hasMetric("servlet_invocationTotalCount{component=\"ejb30_weblogic\",servletName=\"JspServlet\"}", 0));
    }

    @Test
    public void whenResponseLacksServerRuntimes_generateEmptyMetrics() {
        assertThat(scraper.scrape(MBeanSelector.create(getFullMap()), getJsonResponse("{}")), anEmptyMap());
    }

    @Test
    public void generateFromFullResponse() {
        Map<String, Object> metrics = scraper.scrape(MBeanSelector.create(getFullMap()), getJsonResponse(RESPONSE));
        
        assertThat(metrics, hasMetric("component_deploymentState{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(metrics, hasMetric("servlet_invocationTotalCount{application=\"weblogic\",component=\"ejb30_weblogic\",servletName=\"JspServlet\"}", 0));
    }

    private Map<String, Object> getFullMap() {
        return ImmutableMap.of("applicationRuntimes",
                    ImmutableMap.of(MBeanSelector.KEY_NAME, "application", MBeanSelector.KEY, "name",
                                    "componentRuntimes", componentMap));
    }


    @Test
    public void generateFromFullResponseUsingSnakeCase() {
        scraper.setMetricNameSnakeCase(true);
        Map<String, Object> metrics = scraper.scrape(MBeanSelector.create(getFullMap()), getJsonResponse(RESPONSE));

        assertThat(metrics, hasMetric("component_deployment_state{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(metrics, hasMetric("servlet_invocation_total_count{application=\"weblogic\",component=\"ejb30_weblogic\",servletName=\"JspServlet\"}", 0));
    }

    @Test
    public void whenTypeNotSpecified_includeAllComponents() {
        Map<String, Object> metrics = scraper.scrape(MBeanSelector.create(getFullMap()), getJsonResponse(RESPONSE));

        assertThat(metrics, hasMetric("component_deploymentState{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(metrics, hasMetric("component_deploymentState{application=\"mbeans\",component=\"EjbStatusBean\"}", 2));
    }

    @Test
    public void selectOnlyWebApps() {
        componentMap.put(MBeanSelector.TYPE, "WebAppComponentRuntime");
        Map<String, Object> metrics = scraper.scrape(MBeanSelector.create(getFullMap()), getJsonResponse(RESPONSE));

        assertThat(metrics, hasMetric("component_deploymentState{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(metrics, not(hasMetric("component_deploymentState{application=\"mbeans\",component=\"EjbStatusBean\"}", 2)));
    }

    @Test
    public void whenValuesAtTopLevel_scrapeThem() {
        final MBeanSelector selector = MBeanSelector.create(getMetricsMap());
        final JsonObject jsonResponse = getJsonResponse(METRICS_RESPONSE);
        Map<String, Object> metrics = scraper.scrape(selector, jsonResponse);

        assertThat(metrics, hasMetric("heapSizeCurrent", 123456));
        assertThat(metrics, hasMetric("processCpuLoad", .0028));
    }


    private Map<String, Object> getMetricsMap() {
        return ImmutableMap.of("JVMRuntime",
                    ImmutableMap.of(MBeanSelector.KEY, "name", MBeanSelector.VALUES, new String[]{"processCpuLoad", "heapSizeCurrent"}));
    }


    private static final String METRICS_RESPONSE =
            "{\"JVMRuntime\": {\n" +
            "    \"uptime\": 137762378,\n" +
            "    \"heapSizeCurrent\": 123456,\n" +
            "    \"heapFreeCurrent\": 287379152,\n" +
            "    \"heapSizeMax\": 477626368,\n" +
            "    \"heapFreePercent\": 69,\n" +
            "    \"processCpuLoad\": 0.0028\n" +
            "}}";

}
