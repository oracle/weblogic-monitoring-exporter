package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.gson.JsonObject;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the configuration for the exporter, created by parsing YAML.
 *
 * @author Russell Gold
 */
public class ExporterConfig {
    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 7001;
    static final String SNAKE_CASE = "metricsNameSnakeCase";
    static final String HOST = "host";
    static final String PORT = "port";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";

    private static final String QUERIES = "queries";
    private static final MBeanSelector[] NO_QUERIES = {};

    private String userName = "";
    private String password = "";
    private MBeanSelector[] queries = {};
    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private boolean metricsNameSnakeCase;

    /**
     * Loads a YAML configuration to create a new configuration object.
     * @param inputStream a reader of a YAML configuration.
     * @return an ExporterConfig object that matches the parsed YAML
     */
    public static ExporterConfig loadConfig(InputStream inputStream) {
        return loadConfig(asMap(new Yaml().load(inputStream)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object yaml) {
        try {
            return (Map<String, Object>) yaml;
        } catch (ClassCastException e) {
            throw new ConfigurationException(ConfigurationException.NOT_YAML_FORMAT);
        }
    }

    /**
     * Creates a set of metrics from a Json object
     *
     * @param selector the description of the metrics to scrape.
     * @param response  a parsed JSON REST response
     * @return a map of metric names to values
     */
    public Map<String, Object> scrapeMetrics(MBeanSelector selector, JsonObject response) {
        MetricsScraper scraper = new MetricsScraper();
        scraper.setMetricNameSnakeCase(metricsNameSnakeCase);
        return scraper.scrape(selector, response);
    }

    /**
     * Returns an array of the mbean selector objects which correspond to the queries section
     * in the YAML.
     * @return an array of mbean selectors.
     */
    public MBeanSelector[] getQueries() {
        return queries == null ? NO_QUERIES : queries;
    }

    public static ExporterConfig loadConfig(Map<String, Object> yamlConfig) {
        if (yamlConfig == null) yamlConfig = new HashMap<>();

        return new ExporterConfig(yamlConfig);
    }

    private ExporterConfig(Map<String, Object> yaml) {
        if (yaml.containsKey(SNAKE_CASE)) setMetricsNameSnakeCase(yaml);
        if (yaml.containsKey(USERNAME)) userName = MapUtils.getStringValue(yaml, USERNAME);
        if (yaml.containsKey(PASSWORD)) password = MapUtils.getStringValue(yaml, PASSWORD);
        if (yaml.containsKey(HOST)) host = MapUtils.getStringValue(yaml, HOST);
        if (yaml.containsKey(PORT)) port = MapUtils.getIntegerValue(yaml, PORT);
        if (yaml.containsKey(QUERIES)) appendQueries(yaml.get(QUERIES));
    }

    private void setMetricsNameSnakeCase(Map<String, Object> yaml) {
        try {
            metricsNameSnakeCase = MapUtils.getBooleanValue(yaml, SNAKE_CASE);
        } catch (ConfigurationException e) {
            e.addContext(SNAKE_CASE);
            throw e;
        }
    }

    private void appendQueries(Object queriesYaml) {
        for (Map<String,Object> selectorSpec : getAsListOfMaps(queriesYaml)) {
            appendQuery(MBeanSelector.create(selectorSpec));
        }
    }

    private void appendQuery(MBeanSelector selector) {
        MBeanSelector queryToReplace = getExistingMergeableQuery(selector);
        if (queryToReplace == null)
            addAsNewQuery(selector);
        else
            replaceQuery(queryToReplace, queryToReplace.merge(selector));
    }

    private MBeanSelector getExistingMergeableQuery(MBeanSelector selector) {
        for (MBeanSelector query : queries)
            if (isMergeableTopLevelQuery(selector, query))
                return query;

        return null;
    }

    private boolean isMergeableTopLevelQuery(MBeanSelector first, MBeanSelector second) {
        if (!first.getNestedSelectors().keySet().equals(second.getNestedSelectors().keySet())) return false;

        for (String key : first.getNestedSelectors().keySet())
            if (!first.getNestedSelectors().get(key).mayMergeWith(second.getNestedSelectors().get(key))) return false;
        
        return true;
    }

    private void replaceQuery(MBeanSelector queryToReplace, MBeanSelector replacement) {
        for (int i = 0; i < queries.length; i++)
            if (queries[i] == queryToReplace)
                queries[i] = replacement;
    }


    private void addAsNewQuery(MBeanSelector selector) {
        MBeanSelector[] newQueries = new MBeanSelector[queries.length + 1];
        System.arraycopy(queries, 0, newQueries, 0, queries.length);
        newQueries[queries.length] = selector;
        queries = newQueries;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAsListOfMaps(Object queriesYaml) {
        if (!isArrayOfMaps(queriesYaml))
            throw MapUtils.createBadTypeException(QUERIES, queriesYaml, "a list of structures");

        return (List<Map<String, Object>>) queriesYaml;
    }

    private boolean isArrayOfMaps(Object object) {
        return List.class.isAssignableFrom(object.getClass()) && emptyOrContainsMaps((List) object);
    }

    private boolean emptyOrContainsMaps(List list) {
        return list.isEmpty() || Map.class.isInstance(list.get(0));
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * Returns true if attribute names should be converted to snake case as metric names
     * @return true if the conversion should be done
     */
    boolean getMetricsNameSnakeCase() {
        return metricsNameSnakeCase;
    }

    /**
     * Sets the host and port explicitly.
     * @param host the host to contact for data
     * @param port the port to contact for data
     */
    public void setServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Appends the queries from the specified configuration
     * @param config2 an additional configuration to combine with this one
     */
    public void append(ExporterConfig config2) {
        for (MBeanSelector query : config2.getQueries())
            appendQuery(query);
    }

    /**
     * Replaces the display rules from the specified configuration. Display rules in
     * the queries and the metricsNameSnakeCase setting.
     * @param config2 a new configuration whose display rules will replace those from this one
     */
    public void replace(ExporterConfig config2) {
        this.metricsNameSnakeCase = config2.metricsNameSnakeCase;
        MBeanSelector[] newQueries = config2.getQueries();
        this.queries = Arrays.copyOf(newQueries, newQueries.length);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("---\n")
                .append("host: ").append(host).append('\n')
                .append("port: ").append(port).append('\n');

        if (metricsNameSnakeCase)
            sb.append("metricsNameSnakeCase: true\n");
        sb.append("queries:\n");

        for (MBeanSelector query : getQueries())
            appendQueryToString(sb, query);

        return sb.toString();
    }

    String toQueryString() {
        StringBuilder sb = new StringBuilder();

        for (MBeanSelector query : getQueries())
            appendQueryToString(sb, query);

        return sb.toString();
    }

    private void appendQueryToString(StringBuilder sb, MBeanSelector query) {
        String indent = "- ";
        List<String> selectorKeys = new ArrayList<>(query.getNestedSelectors().keySet());
        for (String selectorKey : selectorKeys) {
            sb.append(indent).append(selectorKey).append(":\n");
            query.getNestedSelectors().get(selectorKey).appendNestedQuery(sb, "    ");
            indent = "  ";
        }
    }
}
