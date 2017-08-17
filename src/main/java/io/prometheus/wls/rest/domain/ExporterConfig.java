package io.prometheus.wls.rest.domain;

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
    private MBeanSelector[] queries;
    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private boolean metricsNameSnakeCase;

    /**
     * Loads a YAML configuration to create a new configuration object.
     * @param inputStream a reader of a YAML configuration.
     * @return an ExporterConfig object that matches the parsed YAML
     */
    @SuppressWarnings("unchecked")
    public static ExporterConfig loadConfig(InputStream inputStream) {
        return loadConfig((Map<String, Object>) new Yaml().load(inputStream));
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
        if (yaml.containsKey(SNAKE_CASE)) metricsNameSnakeCase = MapUtils.getBooleanValue(yaml, SNAKE_CASE);
        if (yaml.containsKey(USERNAME)) userName = MapUtils.getStringValue(yaml, USERNAME);
        if (yaml.containsKey(PASSWORD)) password = MapUtils.getStringValue(yaml, PASSWORD);
        if (yaml.containsKey(HOST)) host = MapUtils.getStringValue(yaml, HOST);
        if (yaml.containsKey(PORT)) port = MapUtils.getIntegerValue(yaml, PORT);
        if (yaml.containsKey(QUERIES)) queries = readQueries(yaml.get(QUERIES));
    }

    private MBeanSelector[] readQueries(Object queriesYaml) {
        List<MBeanSelector> queries = new ArrayList<>();

        for (Map<String,Object> selectorSpec : getAsListOfMaps(queriesYaml))
            queries.add(MBeanSelector.create(selectorSpec));

        return queries.toArray(new MBeanSelector[queries.size()]);
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
    public boolean getMetricsNameSnakeCase() {
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
        MBeanSelector[] originalQueries = getQueries();
        MBeanSelector[] appendedQueries = config2.getQueries();
        MBeanSelector[] newQueries = new MBeanSelector[originalQueries.length + appendedQueries.length];
        System.arraycopy(originalQueries, 0, newQueries, 0, originalQueries.length);
        System.arraycopy(appendedQueries, 0, newQueries, originalQueries.length, appendedQueries.length);
        this.queries = newQueries;
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
                .append("port: ").append(port).append('\n')
                .append("queries:\n");

        for (MBeanSelector query : getQueries())
            appendQuery(sb, query);

        return sb.toString();
    }

    private void appendQuery(StringBuilder sb, MBeanSelector query) {
        String indent = "- ";
        List<String> selectorKeys = new ArrayList<>(query.getNestedSelectors().keySet());
        for (String selectorKey : selectorKeys) {
            sb.append(indent).append(selectorKey).append(":\n");
            query.getNestedSelectors().get(selectorKey).appendNestedQuery(sb, "    ");
            indent = "  ";
        }
    }
}
