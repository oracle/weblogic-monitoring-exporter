// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.meterware.simplestub.SystemPropertySupport;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

import static com.oracle.wls.exporter.domain.ExporterConfig.DOMAIN_NAME_PROPERTY;
import static com.oracle.wls.exporter.domain.ExporterConfigTest.QueryHierarchyMatcher.hasQueryFor;
import static com.oracle.wls.exporter.domain.MetricMatcher.hasMetric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Russell Gold
 */
class ExporterConfigTest {
    private static final int EXPECTED_PORT = 3456;
    private static final String SERVLET_CONFIG = "---\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    componentRuntimes:\n" +
            "      type: WebAppComponentRuntime\n" +
            "      prefix: webapp_config_\n" +
            "      key: name\n" +
            "      values: [deploymentState, type, contextRoot, sourceInfo, openSessionsHighCount, openSessionsCurrentCount, sessionsOpenedTotalCount]\n" +
            "      servlets:\n" +
            "        prefix: weblogic_servlet_\n" +
            "        key: servletName\n" +
            "        values: [invocationTotalCount, executionTimeTotal]\n";

    private static final Map<String, Object> NULL_MAP = null;

    private Map<String,Object> yamlConfig = new HashMap<>();
    private final List<Memento> mementos = new ArrayList<>();

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        mementos.add(StaticStubSupport.install(ExporterConfig.class, "defaultSnakeCaseSetting", false));
        mementos.add(SystemPropertySupport.preserve(DOMAIN_NAME_PROPERTY));
        System.clearProperty(DOMAIN_NAME_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    void whenYamlConfigEmpty_returnNonNullConfiguration() {
        assertThat(ExporterConfig.loadConfig(NULL_MAP), notNullValue());
    }

    @Test
    void byDefaultSnakeCase_isDisabled() {
        ExporterConfig config = ExporterConfig.loadConfig(NULL_MAP);

        assertThat(config.getMetricsNameSnakeCase(), equalTo(false));
    }

    @Test
    void whenSnakeNameEnabledByDefault_isSetInEmptyConfiguration() {
        ExporterConfig.setDefaultMetricsNameSnakeCase(true);
        ExporterConfig config = ExporterConfig.loadConfig(NULL_MAP);

        assertThat(config.getMetricsNameSnakeCase(), equalTo(true));
    }

    @Test
    void whenYamlConfigEmpty_queryReturnsEmptyArray() {
        ExporterConfig config = ExporterConfig.loadConfig(NULL_MAP);

        assertThat(config.getQueries(), emptyArray());
    }

    @Test
    void whenSpecified_readSnakeCaseSettingFromYaml() {
        yamlConfig.put(ExporterConfig.SNAKE_CASE, true);

        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getMetricsNameSnakeCase(), is(true));
     }

    @Test
    void whenDomainQualifierNotSpecified_dontModifyQueries() {
        ExporterConfig config = loadFromString(SERVLET_CONFIG);

        MBeanSelector[] queries = config.getEffectiveQueries();
        assertThat(queries, arrayWithSize(1));
        assertThat(queries[0].getUrl(Protocol.HTTP, "myhost", 1234),
                equalTo(String.format(QueryType.RUNTIME_URL_PATTERN, "http", "myhost", 1234)));
     }

    @Test
    void whenSpecified_prependConfigurationQuery() {
        ExporterConfig config = loadFromString(DOMAIN_QUALIFIER_CONFIG);

        MBeanSelector[] queries = config.getEffectiveQueries();
        assertThat(queries, arrayWithSize(2));
        assertThat(queries[0], sameInstance(MBeanSelector.DOMAIN_NAME_SELECTOR));
        assertThat(queries[1].getUrl(Protocol.HTTP, "myhost", 1234),
                equalTo(String.format(QueryType.RUNTIME_URL_PATTERN, "http", "myhost", 1234)));
     }

    @Test
    void whenNotSpecified_wlsPortIsNull() {
        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getRestPort(), nullValue());
    }

    @Test
    void whenNotSpecified_wlsPortIsNotIncludedInToString() {
        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.toString(), not(containsString(ExporterConfig.REST_PORT)));
    }

    @Test
    void whenSpecified_readWlsPortFromYaml() {
        yamlConfig.put(ExporterConfig.REST_PORT, EXPECTED_PORT);

        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getRestPort(), equalTo(EXPECTED_PORT));
    }

    @Test
    void whenSpecified_wlsPortIsIncludedInToString() {
        yamlConfig.put(ExporterConfig.REST_PORT, EXPECTED_PORT);

        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.toString(), containsString(ExporterConfig.REST_PORT + ": " + EXPECTED_PORT));
    }

    @Test
    void whenNotSpecified_querySyncConfigurationIsNull() {
        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getQuerySyncConfiguration(), nullValue());
    }

    @Test
    void whenQuerySyncDefinedWithoutProperties_throwException() {
        assertThrows(ConfigurationException.class, () -> loadFromString(CONFIG_WITH_MISSING_SYNC_PROPERTIES));
    }

    private static final String CONFIG_WITH_MISSING_SYNC_PROPERTIES =
            "query_sync:";


    @Test
    void whenQuerySyncDefinedWithoutUrl_throwException() {
        assertThrows(ConfigurationException.class, () -> loadFromString(CONFIG_WITH_MISSING_SYNC_URL));
    }

    private static final String CONFIG_WITH_MISSING_SYNC_URL =
            "query_sync:\n  interval: 3";


    @Test
    void whenQuerySyncDefined_returnIt() {
        ExporterConfig config = loadFromString(CONFIG_WITH_SYNC_SPEC);
        final QuerySyncConfiguration syncConfiguration = config.getQuerySyncConfiguration();

        assertThat(syncConfiguration.getUrl(), equalTo("http://sync:8999/"));
        assertThat(syncConfiguration.getRefreshInterval(), equalTo(3L));
    }

    private static final String CONFIG_WITH_SYNC_SPEC =
            "query_sync:\n  url: http://sync:8999/\n  interval: 3";


    @Test
    void whenQuerySyncDefinedWithoutInterval_useDefault() {
        ExporterConfig config = loadFromString(CONFIG_WITH_SYNC_URL);
        final QuerySyncConfiguration syncConfiguration = config.getQuerySyncConfiguration();

        assertThat(syncConfiguration.getUrl(), equalTo("http://sync:8999/"));
        assertThat(syncConfiguration.getRefreshInterval(), equalTo(10L));
    }

    private static final String CONFIG_WITH_SYNC_URL =
            "query_sync:\n  url: http://sync:8999/\n";

    @Test
    void whenSpecified_readQueriesFromYaml() {
        ExporterConfig config = loadFromString(SERVLET_CONFIG);

        assertThat(config.getQueries(), arrayWithSize(1));
    }

    private ExporterConfig loadFromString(String yamlString) {
        yamlConfig = new Yaml().load(yamlString);

        return ExporterConfig.loadConfig(yamlConfig);
    }

    @Test
    void afterLoad_hasExpectedQuery() {
        ExporterConfig config = loadFromString(SERVLET_CONFIG);

        assertThat(config, hasQueryFor("applicationRuntimes", "componentRuntimes"));
    }

    @Test
    void afterLoad_convertToString() {
        ExporterConfig config = loadFromString(SNAKE_CASE_CONFIG);

        assertThat(config.toString(), equalToCompressingWhiteSpace(SNAKE_CASE_CONFIG));
    }

    @Test
    void whenQueriesNotDefinedAsArray_reportError() {
        assertThrows(ConfigurationException.class, () -> loadFromString(BAD_QUERY_CONFIG));
    }

    private static final String BAD_QUERY_CONFIG =
            "queries:\n" +
            "  key: name\n" +
            "  values: [state, serverStartupTime]\n";

    @Test
    void includeTopLevelFieldsInString() {
        ExporterConfig config = loadFromString(CONFIG_WITH_TOP_LEVEL_FIELDS);

        assertThat(config.toString(), equalToCompressingWhiteSpace(CONFIG_WITH_TOP_LEVEL_FIELDS));
    }
    
    private static final String CONFIG_WITH_TOP_LEVEL_FIELDS =
            "queries:\n" +
            "- key: name\n" +
            "  values: [state, serverStartupTime]\n" +
            "  JVMRuntime:\n" +
            "    prefix: jvm_\n" +
            "    key: name\n" +
            "    values: [heapFreeCurrent, heapFreePercent, heapSizeCurrent]\n";

    @Test
    void includeSnakeCaseTrueSettingInToString() {
        ExporterConfig config = loadFromString(SNAKE_CASE_CONFIG);

        assertThat(config.toString(), equalToCompressingWhiteSpace(SNAKE_CASE_CONFIG));
    }

    private static final String SNAKE_CASE_CONFIG =
            "metricsNameSnakeCase: true\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";

    @Test
    void includeRestPortSettingInToString() {
        ExporterConfig config = loadFromString(REST_PORT_CONFIG);

        assertThat(config.toString(), equalToCompressingWhiteSpace(REST_PORT_CONFIG));
    }

    private static final String REST_PORT_CONFIG =
            "restPort: 1234\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";

    @Test
    void whenRestPortSetAsIntegerString_parseIt() {
        ExporterConfig config = loadFromString(STRING_REST_PORT_CONFIG);

        assertThat(config.getRestPort(), equalTo(1235));

    }

    private static final String STRING_REST_PORT_CONFIG =
          "restPort: '1235'\nqueries:";

    @Test
    void whenRestPortNotParseable_reportError() {
        assertThrows(ConfigurationException.class, () -> loadFromString(JUNK_REST_PORT_CONFIG));
    }

    private static final String JUNK_REST_PORT_CONFIG =
          "restPort: One\nqueries:";

    private ExporterConfig getAppendedConfiguration(String firstConfiguration, String secondConfiguration) {
        ExporterConfig config = loadFromString(firstConfiguration);
        ExporterConfig config2 = loadFromString(secondConfiguration);
        config.append(config2);
        return config;
    }

    private static final String WORK_MANAGER_CONFIG = "---\n" +
            "metricsNameSnakeCase: true\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";

    @Test
    void afterAppend_configHasOriginalSnakeCase() {
        assertThat(getAppendedConfiguration(SERVLET_CONFIG, WORK_MANAGER_CONFIG).getMetricsNameSnakeCase(), is(false));
        assertThat(getAppendedConfiguration(WORK_MANAGER_CONFIG, SERVLET_CONFIG).getMetricsNameSnakeCase(), is(true));
     }

    @Test
    void afterAppend_configHasOriginalRestPort() {
        assertThat(getAppendedConfiguration(SERVLET_CONFIG, REST_PORT_CONFIG).getRestPort(), nullValue());
        assertThat(getAppendedConfiguration(REST_PORT_CONFIG, SERVLET_CONFIG).getRestPort(), equalTo(1234));
     }

    @Test
    void afterAppend_configHasOriginalQuery() {
        ExporterConfig config = getAppendedConfiguration(SERVLET_CONFIG, WORK_MANAGER_CONFIG);

        assertThat(config, hasQueryFor("applicationRuntimes", "componentRuntimes", "servlets"));
    }

    @Test
    void afterAppend_configContainsNewQuery() {
        ExporterConfig config = getAppendedConfiguration(SERVLET_CONFIG, WORK_MANAGER_CONFIG);

        assertThat(config, hasQueryFor("applicationRuntimes", "workManagerRuntimes"));
    }

    @Test
    void whenAppendToNoQueries_configHasNewQuery() {
        ExporterConfig config = getAppendedConfiguration("", WORK_MANAGER_CONFIG);

        assertThat(config, hasQueryFor("applicationRuntimes", "workManagerRuntimes"));
    }

    @Test
    void whenAppendedConfigurationHasNoQueries_configHasOriginalQuery() {
        ExporterConfig config = getAppendedConfiguration(SERVLET_CONFIG, "");

        assertThat(config, hasQueryFor("applicationRuntimes", "componentRuntimes", "servlets"));
    }

    private ExporterConfig getReplacedConfiguration(String firstConfiguration, String secondConfiguration) {
        ExporterConfig config = loadFromString(firstConfiguration);
        ExporterConfig config2 = loadFromString(secondConfiguration);
        config.replace(config2);
        return config;
    }

    @Test
    void afterReplace_configHasChangedSnakeCase() {
        assertThat(getReplacedConfiguration(SERVLET_CONFIG, WORK_MANAGER_CONFIG).getMetricsNameSnakeCase(), is(true));
        assertThat(getReplacedConfiguration(WORK_MANAGER_CONFIG, SERVLET_CONFIG).getMetricsNameSnakeCase(), is(false));
     }

    @Test
    void afterReplace_configHasChangedRestPort() {
        assertThat(getReplacedConfiguration(SERVLET_CONFIG, REST_PORT_CONFIG).getRestPort(), equalTo(1234));
        assertThat(getReplacedConfiguration(REST_PORT_CONFIG, SERVLET_CONFIG).getRestPort(), nullValue());
     }

    @Test
    void afterReplace_configHasChangedDomainQualifier() {
        assertThat(getReplacedConfiguration(SERVLET_CONFIG, DOMAIN_QUALIFIER_CONFIG).useDomainQualifier(), is(true));
        assertThat(getReplacedConfiguration(DOMAIN_QUALIFIER_CONFIG, SERVLET_CONFIG).useDomainQualifier(), is(false));
     }

    @Test
    void afterReplace_configHasNewQuery() {
        ExporterConfig config = getReplacedConfiguration(SERVLET_CONFIG, WORK_MANAGER_CONFIG);

        assertThat(config, hasQueryFor("applicationRuntimes", "workManagerRuntimes"));
    }

    @Test
    void afterReplace_configDoesNotHaveOriginalQuery() {
        ExporterConfig config = getReplacedConfiguration(SERVLET_CONFIG, WORK_MANAGER_CONFIG);

        assertThat(config, not(hasQueryFor("applicationRuntimes", "componentRuntimes", "servlets")));
    }

    @Test
    void afterReplaceWithEmptyConfig_configHasNoQueries() {
        ExporterConfig config = getReplacedConfiguration(SERVLET_CONFIG, "");

        assertThat(config.getQueries(), arrayWithSize(0));
    }


    @Test
    void whenYamlContainsMergeableQueries_MergeThem() {
        ExporterConfig config = loadFromString(MERGEABLE_CONFIG);

        assertThat(config.toString(), equalTo(MERGED_CONFIG));
    }

    private static final String MERGEABLE_CONFIG =
            "metricsNameSnakeCase: true\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    componentRuntimes:\n" +
            "      type: WebAppComponentRuntime\n" +
            "      prefix: webapp_config_\n" +
            "      key: name\n" +
            "      values: [deploymentState, type, contextRoot, sourceInfo, openSessionsHighCount, openSessionsCurrentCount, sessionsOpenedTotalCount]\n" +
            "      servlets:\n" +
            "        prefix: weblogic_servlet_\n" +
            "        key: servletName\n" +
            "        values: [invocationTotalCount, executionTimeTotal]\n";

    private static final String MERGED_CONFIG =
            "metricsNameSnakeCase: true\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n" +
            "    componentRuntimes:\n" +
            "      type: WebAppComponentRuntime\n" +
            "      prefix: webapp_config_\n" +
            "      key: name\n" +
            "      values: [deploymentState, type, contextRoot, sourceInfo, openSessionsHighCount, openSessionsCurrentCount, sessionsOpenedTotalCount]\n" +
            "      servlets:\n" +
            "        prefix: weblogic_servlet_\n" +
            "        key: servletName\n" +
            "        values: [invocationTotalCount, executionTimeTotal]\n";

    @Test
    void afterAppendWithNewTopLevelQuery_configHasMultipleTopLevelQueries() {
        ExporterConfig config = getAppendedConfiguration(SERVLET_CONFIG, PARTITION_CONFIG);

        assertThat(config.getQueries(), arrayWithSize(2));
    }

    private static final String PARTITION_CONFIG =
            "queries:\n" +
            "- partitionRuntimes:\n" +
            "    key: name\n" +
            "    keyName: partition\n" +
            "    applicationRuntimes:\n" +
            "      key: name\n" +
            "      workManagerRuntimes:\n" +
            "        prefix: workmanager_\n" +
            "        key: applicationName\n" +
            "        values: [pendingRequests, completedRequests, stuckThreadCount]\n";


    @Test
    void afterAppendWithMatchingTopLevelQuery_configHasMergedQueries() {
        ExporterConfig config = getAppendedConfiguration(SERVLET_CONFIG, WORK_MANAGER_CONFIG);

        assertThat(config.getQueries(), arrayWithSize(1));
    }

    @Test
    void whenConfigHasSingleValue_displayAsScalar() {
        ExporterConfig exporterConfig = loadFromString(CONFIG_WITH_SINGLE_VALUE);

        assertThat(exporterConfig.toString(), equalTo(CONFIG_WITH_SINGLE_VALUE));
    }


    private static final String CONFIG_WITH_SINGLE_VALUE =
            "queries:\n" +
            "- JVMRuntime:\n" +
            "    key: name\n" +
            "    values: heapFreeCurrent\n";

    @Test
    void whenConfigHasDuplicateValues_reportFailure() {
        assertThrows(ConfigurationException.class, () -> loadFromString(CONFIG_WITH_DUPLICATE_VALUE));
    }

    private static final String CONFIG_WITH_DUPLICATE_VALUE =
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [heapFreeCurrent,heapFreeCurrent]\n";

    @Test
    void whenConfigHasNoValues_reportFailure() {
        assertThrows(ConfigurationException.class, () -> loadFromString(CONFIG_WITH_NO_VALUES));
    }

    private static final String CONFIG_WITH_NO_VALUES =
            "queries:\n" +
            "- JVMRuntime:\n" +
            "    key: name\n" +
            "    values: []\n";

    @Test
    void whenConfigHasDuplicateStringValueKeys_reportFailure() {
        assertThrows(ConfigurationException.class, () -> loadFromString(CONFIG_WITH_DUPLICATE_STRING_VALUES));
    }

    private static final String CONFIG_WITH_DUPLICATE_STRING_VALUES =
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      stringValues:\n" +
            "        state: [open, close]\n" +
            "        color: [red, green, red]";

    @Test
    void whenNoKeySpecifiedButSelectedKeysSpecified_reportFailure() {
        assertThrows(ConfigurationException.class, () -> loadFromString(CONFIG_WITH_IMPOSSIBLE_SELECTED_KEYS));
    }

    private static final String CONFIG_WITH_IMPOSSIBLE_SELECTED_KEYS =
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      selectedKeys: [app1, app2]";

    @Test
    void whenConfigHasDuplicateSelectedKeys_reportFailure() {
        assertThrows(ConfigurationException.class, () -> loadFromString(CONFIG_WITH_DUPLICATE_SELECTED_KEYS));
    }

    private static final String CONFIG_WITH_DUPLICATE_SELECTED_KEYS =
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      selectedKeys: [app1, app2, app1]";

    @Test
    void defineEmptyConfiguration() {
        assertThat(ExporterConfig.createEmptyConfig().toString(), equalTo(EMPTY_CONFIG));
    }

    private static final String EMPTY_CONFIG =
            "queries:\n";

    @Test
    void afterReplaceEmptyConfig_haveReplacementConfig() {
        ExporterConfig config = ExporterConfig.createEmptyConfig();

        config.replace(loadFromString(PARTITION_CONFIG));

        assertThat(config.toString(), equalTo(PARTITION_CONFIG));
    }

    @Test
    void afterAppendEmptyConfig_haveAppendedConfig() {
        ExporterConfig config = ExporterConfig.createEmptyConfig();

        config.append(loadFromString(PARTITION_CONFIG));

        assertThat(config.toString(), equalTo(PARTITION_CONFIG));
    }

    @Test
    void whenEnvironmentVariableDefined_configHasDomainName() {
        System.setProperty(DOMAIN_NAME_PROPERTY, "envDomain");
        ExporterConfig exporterConfig = loadFromString(DOMAIN_QUALIFIER_CONFIG);

        assertThat(exporterConfig.getDomainName(), equalTo("envDomain"));
    }

    @Test
    void afterScrapingServerConfig_hasDomainName() {
        ExporterConfig exporterConfig = loadFromString(DOMAIN_QUALIFIER_CONFIG);

        exporterConfig.scrapeMetrics(MBeanSelector.DOMAIN_NAME_SELECTOR, getJsonResponse(CONFIG_RESPONSE));
        
        assertThat(exporterConfig.getDomainName(), equalTo("mydomain"));
    }

    @Test
    void afterScrapingMetricsIncludeDomainNameQualifier() {
        ExporterConfig exporterConfig = loadFromString(DOMAIN_QUALIFIER_CONFIG);

        Map<String, Object> metrics = getMetrics(exporterConfig);

        assertThat(metrics, hasMetric("workmanager_pendingRequests{domain=\"mydomain\",applicationName=\"thisOne\"}", 2));
        assertThat(metrics, hasMetric("workmanager_completedRequests{domain=\"mydomain\",applicationName=\"thisOne\"}", 15));
        assertThat(metrics, hasMetric("workmanager_stuckThreadCount{domain=\"mydomain\",applicationName=\"thisOne\"}", 3));
    }

    private Map<String, Object> getMetrics(ExporterConfig exporterConfig) {
        Map<String, Object> metrics = new HashMap<>();
        Arrays.stream(exporterConfig.getEffectiveQueries())
              .forEach(q -> metrics.putAll(exporterConfig.scrapeMetrics(q, getJsonResponse(getResponse(q)))));
        return metrics;
    }

    private String getResponse(MBeanSelector selector) {
        return selector.acceptsStrings() ? CONFIG_RESPONSE : WORK_MANAGER_RESPONSE;
    }

    @Test
    void secondScrapeWithDomainQualifierDoesNotAddStringMetric() {
        ExporterConfig exporterConfig = loadFromString(DOMAIN_QUALIFIER_CONFIG);
        getMetrics(exporterConfig);

        assertThat(getMetrics(exporterConfig).values().stream().anyMatch(v -> v instanceof String), is(false));
    }

    private static final String DOMAIN_QUALIFIER_CONFIG =
            ExporterConfig.DOMAIN_QUALIFIER + ": true\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";

    private static final String CONFIG_RESPONSE = "{\"name\": \"mydomain\"}";

    private static final String WORK_MANAGER_RESPONSE =
            "{\"applicationRuntimes\": {\"items\": [\n" +
            "     {\n" +
            "            \"internal\": false,\n" +
            "            \"name\": \"mbeans\",\n" +
            "            \"workManagerRuntimes\": {\"items\": [{\n" +
            "                \"applicationName\": \"thisOne\",\n" +
            "                \"pendingRequests\": 2,\n" +
            "                \"completedRequests\": 15,\n" +
            "                \"stuckThreadCount\": 3\n" +
            "              }]}\n" +
            "     }\n" +
            "]}}";

    @Test
    void whenNullSelectedKeysSpecified_reportError() {
        assertThrows(ConfigurationException.class, () -> loadFromString(NULL_ARRAY_FILTERED_CONFIG));
    }

    private static final String NULL_ARRAY_FILTERED_CONFIG =
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    workManagerRuntimes:\n" +
            "      key: applicationName\n" +
            "      selectedKeys: \n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";

    @Test
    void whenEmptySelectedKeysSpecified_reportError() {
        assertThrows(ConfigurationException.class, () -> loadFromString(EMPTY_ARRAY_FILTERED_CONFIG));
    }

    private static final String EMPTY_ARRAY_FILTERED_CONFIG =
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    workManagerRuntimes:\n" +
            "      key: applicationName\n" +
            "      selectedKeys: []\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";

    @Test
    void whenConfigHasSelectedKeys_displayThem() {
        ExporterConfig exporterConfig = loadFromString(FILTERED_CONFIG);

        assertThat(exporterConfig.toString(), containsString("selectedKeys: [first, second]"));
    }

    private static final String FILTERED_CONFIG =
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    workManagerRuntimes:\n" +
            "      key: applicationName\n" +
            "      selectedKeys: [first, second]\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";


    @SuppressWarnings("SameParameterValue")
    private JsonObject getJsonResponse(String jsonString) {
        return JsonParser.parseString(jsonString).getAsJsonObject();
    }

    @SuppressWarnings("unused")
    static class QueryHierarchyMatcher extends TypeSafeDiagnosingMatcher<ExporterConfig> {
        private final String[] selectorKeys;

        private QueryHierarchyMatcher(String[] selectorKeys) {
            this.selectorKeys = selectorKeys;
        }

        static QueryHierarchyMatcher hasQueryFor(String... selectorKeys) {
            return new QueryHierarchyMatcher(selectorKeys);
        }

        @Override
        protected boolean matchesSafely(ExporterConfig config, Description description) {
            for (MBeanSelector mBeanSelector : config.getQueries())
                if (matchesQuery(mBeanSelector, selectorKeys)) return true;

            reportMismatch(config, description);
            return false;
        }

        private boolean matchesQuery(MBeanSelector mBeanSelector, String[] selectorKeys) {
            MBeanSelector selector = mBeanSelector;
            for (String selectorKey : selectorKeys) {
                MBeanSelector nestedSelector = selector.getNestedSelectors().get(selectorKey);
                if (nestedSelector == null) return false;
                selector = nestedSelector;
            }
            return true;
        }

        private void reportMismatch(ExporterConfig config, Description description) {
            description.appendText("configuration has queries:\n ").appendText(config.toQueryString());
        }

        @Override
        public void describeTo(Description description) {
            description.appendValueList("query for [", ", ", "]", selectorKeys);
        }
    }


}
