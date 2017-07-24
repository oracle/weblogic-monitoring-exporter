package io.prometheus.wls.rest.domain;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MetricsScraperTest {
    private static final String RESPONSE =
            "{\"serverRuntimes\": {\"items\": [{\n" +
            "    \"name\": \"ejb30mtflexadmin\",\n" +
            "    \"applicationRuntimes\": {\"items\": [\n" +
            "        {\n" +
            "            \"internal\": false,\n" +
            "            \"name\": \"mbeans\",\n" +
            "            \"componentRuntimes\": {\"items\": [{\n" +
            "                \"deploymentState\": 2,\n" +
            "                \"name\": \"EjbStatusBean\",\n" +
            "                \"type\": \"EJBComponentRuntime\"\n" +
            "              }]}\n" +
            "        },\n" +
            "        {\n" +
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
            "              }]}\n" +
            "        }\n" +
            "      ]}\n" +
            "}]}}";

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
            "                        \"invocationTotalCount\": 0\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"FileServlet\",\n" +
            "                        \"invocationTotalCount\": 1\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"servletName\": \"ready\",\n" +
            "                        \"invocationTotalCount\": 2\n" +
            "                    }\n" +
            "                ]}\n" +
            "            }";

    private static final String SINGLE_SERVLET_RESPONSE = "{\n" +
            "                \"servlets\": {\"items\": [\n" +
            "                    {\n" +
            "                        \"servletName\": \"JspServlet\",\n" +
            "                        \"invocationTotalCount\": 0\n" +
            "                    }\n" +
            "                ]}\n" +
            "            }";

    private final Map<String,Object> leafMap = new HashMap<>(ImmutableMap.of(MBeanSelector.KEY, "servletName",
            MBeanSelector.PREFIX, "servlet_", MBeanSelector.VALUES, new String[]{"invocationTotalCount"}));

    private final Map<String,Object> componentMap = new HashMap<>(
            ImmutableMap.of(MBeanSelector.KEY_NAME, "component", MBeanSelector.KEY, "name", MBeanSelector.PREFIX, "component_",
                            "servlets", leafMap,
                            MBeanSelector.VALUES, new String[]{"sourceInfo", "internal", "deploymentState"}));

    private final Map<String,Object> twoLevelMap = ImmutableMap.of("componentRuntimes", componentMap);

    private final MetricsScraper generator = new MetricsScraper();

    @Test
    public void generateLeafMetrics() throws Exception {
        generateNestedMetrics(getServletsMap(), SERVLET_RESPONSE);

        assertThat(generator.getMetrics(),
                   allOf(hasEntry("servlet_invocationTotalCount{servletName=\"JspServlet\"}", 0),
                         hasEntry("servlet_invocationTotalCount{servletName=\"FileServlet\"}", 1),
                         hasEntry("servlet_invocationTotalCount{servletName=\"ready\"}", 2)));
    }

    private ImmutableMap<String, Object> getServletsMap() {
        return ImmutableMap.of("servlets", leafMap);
    }

    private void generateNestedMetrics(Map<String,Object> map, String jsonString) {
        generateNestedMetrics(map, jsonString, "");
    }

    private void generateNestedMetrics(Map<String,Object> map, String jsonString, String parentQualifiers) {
        generator.scrapeSubObject(getJsonResponse(jsonString), MBeanSelector.create(map), parentQualifiers);
    }

    private JsonObject getJsonResponse(String jsonString) {
        return new JsonParser().parse(jsonString).getAsJsonObject();
    }

    @Test
    public void generateLeafMetricsWhileAccumulatingQualifiers() throws Exception {
        generateNestedMetrics(getServletsMap(), SERVLET_RESPONSE, "webapp=\"wls\"");

        assertThat(generator.getMetrics(),
                   hasEntry("servlet_invocationTotalCount{webapp=\"wls\",servletName=\"JspServlet\"}", 0));
    }

    @Test
    public void generateLeafMetricsWithNoQualifiers() throws Exception {
        generateNestedMetrics(getServletsMapWithoutQualifierKey(), SINGLE_SERVLET_RESPONSE);

        assertThat(generator.getMetrics(), hasEntry("servlet_invocationTotalCount", 0));
    }

    private ImmutableMap<String, Object> getServletsMapWithoutQualifierKey() {
        leafMap.remove(MBeanSelector.KEY);
        return ImmutableMap.of("servlets", leafMap);
    }

    @Test
    public void generateLeafMetricsWithParentQualifiersOnly() throws Exception {
        generateNestedMetrics(getServletsMapWithoutQualifierKey(), SINGLE_SERVLET_RESPONSE, "webapp=\"wls\"");

        assertThat(generator.getMetrics(), hasEntry("servlet_invocationTotalCount{webapp=\"wls\"}", 0));
    }

    @Test
    public void whenKeyNameSpecified_useItRatherThanKey() throws Exception {
        generateNestedMetrics(getServletsMapWithKeyName("servlet"), SERVLET_RESPONSE);

        assertThat(generator.getMetrics(), hasEntry("servlet_invocationTotalCount{servlet=\"JspServlet\"}", 0));
    }

    @SuppressWarnings("SameParameterValue")
    private Map<String, Object> getServletsMapWithKeyName(String keyName) {
        leafMap.put(MBeanSelector.KEY_NAME, keyName);
        return ImmutableMap.of("servlets", leafMap);
    }

    @Test
    public void whenGenerateHierarchicalMetrics_containsTopLevel() throws Exception {
        generateNestedMetrics(twoLevelMap, TWO_LEVEL_RESPONSE);

        assertThat(generator.getMetrics(),
                   allOf(hasEntry("component_sourceInfo{component=\"ejb30_weblogic\"}", "weblogic.war"),
                         hasEntry("component_internal{component=\"ejb30_weblogic\"}", "true")));
    }

    @Test
    public void whenGenerateHierarchicalMetrics_containsBottomLevel() throws Exception {
        generateNestedMetrics(twoLevelMap, TWO_LEVEL_RESPONSE);

        assertThat(generator.getMetrics(), hasEntry("servlet_invocationTotalCount{component=\"ejb30_weblogic\",servletName=\"JspServlet\"}", 0));
    }

    @Test
    public void generateFromFullResponse() throws Exception {
        generator.scrape(MBeanSelector.create(getFullMap()), getJsonResponse(RESPONSE));

        assertThat(generator.getMetrics(), hasEntry("component_sourceInfo{application=\"weblogic\",component=\"ejb30_weblogic\"}", "weblogic.war"));
        assertThat(generator.getMetrics(), hasEntry("component_deploymentState{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(generator.getMetrics(), hasEntry("servlet_invocationTotalCount{application=\"weblogic\",component=\"ejb30_weblogic\",servletName=\"JspServlet\"}", 0));
    }

    private Map<String, Object> getFullMap() {
        return ImmutableMap.of("applicationRuntimes",
                    ImmutableMap.of(MBeanSelector.KEY_NAME, "application", MBeanSelector.KEY, "name",
                                    "componentRuntimes", componentMap));
    }

    @Test
    public void generateFromFullResponseWithExplicitParentInSelector() throws Exception {
        generator.scrape(MBeanSelector.create(getFullMapWithExplicitParent()), getJsonResponse(RESPONSE));

        assertThat(generator.getMetrics(), hasEntry("component_sourceInfo{application=\"weblogic\",component=\"ejb30_weblogic\"}", "weblogic.war"));
        assertThat(generator.getMetrics(), hasEntry("component_deploymentState{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(generator.getMetrics(), hasEntry("servlet_invocationTotalCount{application=\"weblogic\",component=\"ejb30_weblogic\",servletName=\"JspServlet\"}", 0));
    }

    private Map<String, Object> getFullMapWithExplicitParent() {
        return ImmutableMap.of("serverRuntimes", getFullMap());
    }


    @Test
    public void whenTypeNotSpecified_includeAllComponents() throws Exception {
        generator.scrape(MBeanSelector.create(getFullMap()), getJsonResponse(RESPONSE));

        assertThat(generator.getMetrics(), hasEntry("component_deploymentState{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(generator.getMetrics(), hasEntry("component_deploymentState{application=\"mbeans\",component=\"EjbStatusBean\"}", 2));
    }

    @Test
    public void selectOnlyWebApps() throws Exception {
        componentMap.put(MBeanSelector.TYPE, "WebAppComponentRuntime");
        generator.scrape(MBeanSelector.create(getFullMap()), getJsonResponse(RESPONSE));

        assertThat(generator.getMetrics(), hasEntry("component_deploymentState{application=\"weblogic\",component=\"ejb30_weblogic\"}", 2));
        assertThat(generator.getMetrics(), not(hasEntry("component_deploymentState{application=\"mbeans\",component=\"EjbStatusBean\"}", 2)));
    }
}
