package io.prometheus.wls.rest.domain;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ExporterConfigTest {
    private static final String EXPECTED_HOST = "somehost";
    private static final int EXPECTED_PORT = 3456;
    private static final String EXPECTED_USERNAME = "testuser";
    private static final String EXPECTED_PASSWORD = "letmein";
    private static final String YAML_STRING = "---\n" +
            "host: " + EXPECTED_HOST + "\n" +
            "port: " + EXPECTED_PORT + "\n" +
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

    @Test
    public void whenYamlConfigEmpty_returnNonNullConfiguration() throws Exception {
        assertThat(ExporterConfig.loadConfig(NULL_MAP), notNullValue());
    }

    @Test
    public void whenYamlConfigEmpty_returnDefaultConfiguration() throws Exception {
        ExporterConfig config = ExporterConfig.loadConfig(NULL_MAP);

        assertThat(config.getMetricsNameSnakeCase(), equalTo(false));
        assertThat(config.getUserName(), emptyString());
        assertThat(config.getPassword(), emptyString());
    }

    @Test
    public void whenYamlConfigEmpty_queryReturnsEmptyArray() throws Exception {
        ExporterConfig config = ExporterConfig.loadConfig(NULL_MAP);

        assertThat(config.getQueries(), emptyArray());
    }

    @Test
    public void whenSpecified_readSnakeCaseSettingFromYaml() throws Exception {
        yamlConfig.put(ExporterConfig.SNAKE_CASE, true);

        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getMetricsNameSnakeCase(), is(true));
     }

    @Test
    public void whenSpecified_readUserNameAndPasswordFromYaml() throws Exception {
        yamlConfig.put(ExporterConfig.USERNAME, EXPECTED_USERNAME);
        yamlConfig.put(ExporterConfig.PASSWORD, EXPECTED_PASSWORD);

        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getUserName(), equalTo(EXPECTED_USERNAME));
        assertThat(config.getPassword(), equalTo(EXPECTED_PASSWORD));
    }

    @Test
    public void whenSpecified_readHostAndPortFromYaml() throws Exception {
        yamlConfig.put(ExporterConfig.HOST, EXPECTED_HOST);
        yamlConfig.put(ExporterConfig.PORT, EXPECTED_PORT);

        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getHost(), equalTo(EXPECTED_HOST));
        assertThat(config.getPort(), equalTo(EXPECTED_PORT));
    }

    @Test
    public void whenNotSpecified_useDefaultHostAndPort() throws Exception {
        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);

        assertThat(config.getHost(), equalTo(ExporterConfig.DEFAULT_HOST));
        assertThat(config.getPort(), equalTo(ExporterConfig.DEFAULT_PORT));
    }

    @Test
    public void setHostAndPort() throws Exception {
        ExporterConfig config = ExporterConfig.loadConfig(yamlConfig);
        config.setServer(EXPECTED_HOST, EXPECTED_PORT);

        assertThat(config.getHost(), equalTo(EXPECTED_HOST));
        assertThat(config.getPort(), equalTo(EXPECTED_PORT));
    }

    @Test
    public void whenSpecified_readQueriesFromYaml() throws Exception {
        ExporterConfig config = loadFromString(YAML_STRING);

        assertThat(config.getQueries(), arrayWithSize(1));
    }

    @SuppressWarnings("unchecked")
    private ExporterConfig loadFromString(String yamlString) {
        yamlConfig = (Map<String, Object>) new Yaml().load(yamlString);

        return ExporterConfig.loadConfig(yamlConfig);
    }

    @Test
    public void topOfParsedSelectorIsApplicationRuntimes() throws Exception {
        ExporterConfig config = loadFromString(YAML_STRING);

        assertThat(config.getQueries()[0].getNestedSelectors().keySet(), contains("applicationRuntimes"));
    }

    @Test
    public void firstNestedSelectedIsComponentRuntimes() throws Exception {
        ExporterConfig config = loadFromString(YAML_STRING);

        MBeanSelector applicationRuntimes = config.getQueries()[0].getNestedSelectors().get("applicationRuntimes");
        assertThat(applicationRuntimes.getNestedSelectors().keySet(), contains("componentRuntimes"));
    }

    @Test
    public void afterLoad_convertToString() throws Exception {
        ExporterConfig config = loadFromString(YAML_STRING);

        assertThat(config.toString(), equalToIgnoringWhiteSpace(YAML_STRING));
    }

    @Test
    public void includeSnakeCaseTrueSettingInToString() throws Exception {
        ExporterConfig config = loadFromString(YAML_STRING2);

        assertThat(config.toString(), equalToIgnoringWhiteSpace(YAML_STRING2));
    }

    @Test
    public void afterAppend_configHasOriginalDestination() throws Exception {
        ExporterConfig config = getAppendedConfiguration(YAML_STRING, YAML_STRING2);

        assertThat(config.getHost(), equalTo(EXPECTED_HOST));
        assertThat(config.getPort(), equalTo(EXPECTED_PORT));
    }

    private ExporterConfig getAppendedConfiguration(String firstConfiguration, String secondConfiguration) {
        ExporterConfig config = loadFromString(firstConfiguration);
        ExporterConfig config2 = loadFromString(secondConfiguration);
        config.append(config2);
        return config;
    }

    private static final String YAML_STRING2 = "---\n" +
            "host: otherhost\n" +
            "port: 9876\n" +
            "metricsNameSnakeCase: true\n" +
            "queries:\n" +
            "- applicationRuntimes:\n" +
            "    key: name\n" +
            "    workManagerRuntimes:\n" +
            "      prefix: workmanager_\n" +
            "      key: applicationName\n" +
            "      values: [pendingRequests, completedRequests, stuckThreadCount]\n";

    @Test
    public void afterAppend_configHasOriginalSnakeCase() throws Exception {
        assertThat(getAppendedConfiguration(YAML_STRING, YAML_STRING2).getMetricsNameSnakeCase(), is(false));
        assertThat(getAppendedConfiguration(YAML_STRING2, YAML_STRING).getMetricsNameSnakeCase(), is(true));
     }

    @Test
    public void afterAppend_configHasOriginalQuery() throws Exception {
        ExporterConfig config = getAppendedConfiguration(YAML_STRING, YAML_STRING2);

        MBeanSelector applicationRuntimes = config.getQueries()[0].getNestedSelectors().get("applicationRuntimes");
        assertThat(applicationRuntimes.getNestedSelectors().keySet(), contains("componentRuntimes"));
    }

    @Test
    public void afterAppend_configHasAdditionalQuery() throws Exception {
        ExporterConfig config = getAppendedConfiguration(YAML_STRING, YAML_STRING2);

        assertThat(config.getQueries(), arrayWithSize(2));
        MBeanSelector applicationRuntimes = config.getQueries()[1].getNestedSelectors().get("applicationRuntimes");
        assertThat(applicationRuntimes.getNestedSelectors().keySet(), contains("workManagerRuntimes"));
    }

    @Test
    public void whenAppendToNoQueries_configHasNewQueryOnly() throws Exception {
        ExporterConfig config = getAppendedConfiguration("", YAML_STRING2);

        assertThat(config.getQueries(), arrayWithSize(1));
        MBeanSelector applicationRuntimes = config.getQueries()[0].getNestedSelectors().get("applicationRuntimes");
        assertThat(applicationRuntimes.getNestedSelectors().keySet(), contains("workManagerRuntimes"));
    }

    @Test
    public void whenAppendedConfigurationHasNoQueries_configHasOriginalQueryOnly() throws Exception {
        ExporterConfig config = getAppendedConfiguration(YAML_STRING, "");

        assertThat(config.getQueries(), arrayWithSize(1));
        MBeanSelector applicationRuntimes = config.getQueries()[0].getNestedSelectors().get("applicationRuntimes");
        assertThat(applicationRuntimes.getNestedSelectors().keySet(), contains("componentRuntimes"));
    }

    @Test
    public void afterReplace_configHasOriginalDestination() throws Exception {
        ExporterConfig config = getReplacedConfiguration(YAML_STRING, YAML_STRING2);

        assertThat(config.getHost(), equalTo(EXPECTED_HOST));
        assertThat(config.getPort(), equalTo(EXPECTED_PORT));
    }

    private ExporterConfig getReplacedConfiguration(String firstConfiguration, String secondConfiguration) {
        ExporterConfig config = loadFromString(firstConfiguration);
        ExporterConfig config2 = loadFromString(secondConfiguration);
        config.replace(config2);
        return config;
    }

    @Test
    public void afterReplace_configHasChangedSnakeCase() throws Exception {
        assertThat(getReplacedConfiguration(YAML_STRING, YAML_STRING2).getMetricsNameSnakeCase(), is(true));
        assertThat(getReplacedConfiguration(YAML_STRING2, YAML_STRING).getMetricsNameSnakeCase(), is(false));
     }

    @Test
    public void afterReplace_configHasReplacedQuery() throws Exception {
        ExporterConfig config = getReplacedConfiguration(YAML_STRING, YAML_STRING2);

        assertThat(config.getQueries(), arrayWithSize(1));
        MBeanSelector applicationRuntimes = config.getQueries()[0].getNestedSelectors().get("applicationRuntimes");
        assertThat(applicationRuntimes.getNestedSelectors().keySet(), contains("workManagerRuntimes"));
    }

    @Test
    public void afterReplaceWithEmptyConfig_configHasNoQueries() throws Exception {
        ExporterConfig config = loadFromString(YAML_STRING);
        ExporterConfig config2 = loadFromString("");
        config.replace(config2);

        assertThat(config.getQueries(), arrayWithSize(0));
    }
}
