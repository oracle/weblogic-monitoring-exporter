// Copyright (c) 2017, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.scanner.ScannerException;

/**
 * This class represents the configuration for the exporter, created by parsing YAML.
 *
 * @author Russell Gold
 */
public class ExporterConfig implements MetricsProcessor {
    // Due to a bug in the WLS REST API, a query that includes this field will fail if not run with admin privileges.
    // The code thus ensures that it is excluded from all queries.
    private static final String[] FORBIDDEN_FIELDS = {"JDBCServiceRuntime:JDBCDataSourceRuntimeMBeans:properties"};

    public static final String DOMAIN_NAME_PROPERTY = "DOMAIN";

    private static final String QUERY_SYNC = "query_sync";
    static final String SNAKE_CASE = "metricsNameSnakeCase";
    static final String DOMAIN_QUALIFIER = "domainQualifier";
    static final String REST_PORT = "restPort";
    private static final String QUERIES_TAG = "queries";

    private static final MBeanSelector[] NO_QUERIES = {};
    private static final String DOMAIN_NAME_QUALIFIER = "domain=\"%s\"";

    private static boolean defaultSnakeCaseSetting;

    private MBeanSelector[] queries = {};
    private Integer restPort;
    private boolean metricsNameSnakeCase = defaultSnakeCaseSetting;
    private QuerySyncConfiguration querySyncConfiguration;
    private boolean useDomainQualifier;
    private String domainName = System.getProperty(DOMAIN_NAME_PROPERTY);

    /**
     * Creates an empty configuration.
     */
    public static ExporterConfig createEmptyConfig() {
        return new ExporterConfig(new HashMap<>());
    }

    /**
     * Loads a YAML configuration to create a new configuration object.
     * @param inputStream a reader of a YAML configuration.
     * @return an ExporterConfig object that matches the parsed YAML
     */
    public static ExporterConfig loadConfig(InputStream inputStream) {
        try {
            return loadConfig(asMap(new Yaml(new SafeConstructor()).load(inputStream)));
        } catch (ScannerException e) {
            throw new YamlParserException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object yaml) {
        try {
            return (Map<String, Object>) yaml;
        } catch (ClassCastException e) {
            throw new ConfigurationException(ConfigurationException.NOT_YAML_FORMAT);
        }
    }

    public static void setDefaultMetricsNameSnakeCase(boolean defaultValue) {
        defaultSnakeCaseSetting = defaultValue;
    }

    /**
     * Creates a set of metrics from a Json object
     *
     * @param selector the description of the metrics to scrape.
     * @param response  a parsed JSON REST response
     * @return a map of metric names to values
     */
    public Map<String, Object> scrapeMetrics(MBeanSelector selector, JsonObject response) {
        MetricsScraper scraper = new MetricsScraper(getGlobalQualifiers());
        scraper.setMetricNameSnakeCase(metricsNameSnakeCase);
        Map<String, Object> metrics = scraper.scrape(selector, response);
        selector.postProcessMetrics(metrics, this);
        return metrics;
    }

    private String getGlobalQualifiers() {
        return Optional.ofNullable(domainName).map(n->String.format(DOMAIN_NAME_QUALIFIER, n)).orElse("");
    }

    @Override
    public void updateConfiguration(Map<String, Object> metrics) {
        Optional.ofNullable((String) metrics.remove("name")).ifPresent(n-> domainName = n);
    }

    /**
     * Returns the queries needed to create the metrics. May not be the same as the result from {@link #getQueries()}
     * @return an array of mbean selectors.
     */
    public MBeanSelector[] getEffectiveQueries() {
        if (queries == null) return NO_QUERIES;

        return withPossibleDomainNameQuery(Arrays.stream(queries)).toArray(MBeanSelector[]::new);
    }

    /**
     * Returns an array of the mbean selector objects which correspond to the queries section
     * in the YAML.
     * @return an array of mbean selectors.
     */
    public MBeanSelector[] getQueries() {
        if (queries == null) return NO_QUERIES;

        return queries;
    }

    private Stream<MBeanSelector> withPossibleDomainNameQuery(Stream<MBeanSelector> stream) {
        return useDomainQualifier() ? Stream.concat(Stream.of(MBeanSelector.DOMAIN_NAME_SELECTOR), stream) : stream;
    }

    public static ExporterConfig loadConfig(Map<String, Object> yamlConfig) {
        if (yamlConfig == null) yamlConfig = new HashMap<>();

        return new ExporterConfig(yamlConfig);
    }

    public QuerySyncConfiguration getQuerySyncConfiguration() {
        return querySyncConfiguration;
    }

    private ExporterConfig(Map<String, Object> yaml) {
        if (yaml.containsKey(DOMAIN_QUALIFIER)) setDomainQualifier(yaml);
        if (yaml.containsKey(SNAKE_CASE)) setMetricsNameSnakeCase(yaml);
        if (yaml.containsKey(REST_PORT)) restPort = MapUtils.getIntegerValue(yaml, REST_PORT);
        if (yaml.containsKey(QUERY_SYNC)) querySyncConfiguration = loadQuerySync(yaml.get(QUERY_SYNC));
        if (yaml.containsKey(QUERIES_TAG)) appendQueries(asList(yaml.get(QUERIES_TAG)));
    }

    private Object asList(Object value) {
        return Optional.ofNullable(value).orElse(Collections.emptyList());
    }

    private void setMetricsNameSnakeCase(Map<String, Object> yaml) {
        try {
            metricsNameSnakeCase = MapUtils.getBooleanValue(yaml, SNAKE_CASE);
        } catch (ConfigurationException e) {
            e.addContext(SNAKE_CASE);
            throw e;
        }
    }

    private void setDomainQualifier(Map<String, Object> yaml) {
        try {
            useDomainQualifier = MapUtils.getBooleanValue(yaml, DOMAIN_QUALIFIER);
        } catch (ConfigurationException e) {
            e.addContext(DOMAIN_QUALIFIER);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private QuerySyncConfiguration loadQuerySync(Object o) {
        return new QuerySyncConfiguration((Map<String, Object>) o);
    }

    private void appendQueries(Object queriesYaml) {
        for (Map<String,Object> selectorSpec : getAsListOfMaps(queriesYaml)) {
            appendQuery(MBeanSelector.create(selectorSpec).withForbiddenFields(FORBIDDEN_FIELDS));
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
            throw MapUtils.createBadTypeException(QUERIES_TAG, queriesYaml, "a list of structures");

        return (List<Map<String, Object>>) queriesYaml;
    }

    private boolean isArrayOfMaps(Object object) {
        return List.class.isAssignableFrom(object.getClass()) && emptyOrContainsMaps((List<?>) object);
    }

    private boolean emptyOrContainsMaps(List<?> list) {
        return list.isEmpty() || list.get(0) instanceof Map;
    }

    /**
     * Returns the port on which the exporter will contact the REST API, if specified.
     * @return a port number, or null
     */
    public Integer getRestPort() {
        return restPort;
    }

    /**
     * Returns true if attribute names should be converted to snake case as metric names
     * @return true if the conversion should be done
     */
    boolean getMetricsNameSnakeCase() {
        return metricsNameSnakeCase;
    }

    /**
     * Returns true if the domain name should be added as a qualifier for all metrics
     * @return true if the qualifier should be added
     */
    boolean useDomainQualifier() {
        return useDomainQualifier && (domainName == null);
    }

    String getDomainName() {
        return domainName;
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
        this.useDomainQualifier = config2.useDomainQualifier;
        this.restPort = config2.restPort;
        MBeanSelector[] newQueries = config2.getQueries();
        this.queries = Arrays.copyOf(newQueries, newQueries.length);
        resetDomainName();
    }

    public void resetDomainName() {
        this.domainName = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (querySyncConfiguration != null)
            sb.append(querySyncConfiguration);
        if (metricsNameSnakeCase) sb.append("metricsNameSnakeCase: true\n");
        if (useDomainQualifier) sb.append(DOMAIN_QUALIFIER + ": true\n");
        if (restPort != null) sb.append(REST_PORT + ": ").append(restPort).append("\n");
        sb.append("queries:\n");

        for (MBeanSelector query : getQueries())
            sb.append(formatQuery(query));

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
            query.getNestedSelectors().get(selectorKey).appendAsNestedQuery(sb, "    ");
            indent = "  ";
        }
    }

    private String formatQuery(MBeanSelector query) {
        StringBuilder sb = new StringBuilder();
        query.appendAsNestedQuery(sb, "  ");
        sb.replace(0, 1, "-");
        return sb.toString();
    }
}
