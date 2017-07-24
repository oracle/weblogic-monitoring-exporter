package io.prometheus.wls.rest.domain;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the configuration for the exporter, created by parsing YAML.
 */
public class ExporterConfig {
    static final String START_DELAY_SECONDS = "startDelaySeconds";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    private static final String QUERIES = "queries";

    private int startDelaySeconds;
    private String userName = "";
    private String password = "";
    private MBeanSelector[] queries;

    /**
     * Loads a YAML configuration to create a new configuration object.
     * @param reader a reader of a YAML configuration.
     * @return an ExporterConfig object that matches the parsed YAML
     */
    @SuppressWarnings("unchecked")
    public static ExporterConfig loadConfig(Reader reader) {
        return loadConfig((Map<String, Object>) new Yaml().load(reader));
    }

    /**
     * Returns an array of the mbean selector objects which correspond to the queries section
     * in the YAML.
     * @return an array of mbean selectors.
     */
    public MBeanSelector[] getQueries() {
        return queries;
    }

    static ExporterConfig loadConfig(Map<String, Object> yamlConfig) {
        if (yamlConfig == null) yamlConfig = new HashMap<>();

        return new ExporterConfig(yamlConfig);
    }

    private ExporterConfig(Map<String, Object> yaml) {
        if (yaml.containsKey(START_DELAY_SECONDS)) startDelaySeconds = MapUtils.getIntegerValue(yaml, START_DELAY_SECONDS);
        if (yaml.containsKey(USERNAME)) userName = MapUtils.getStringValue(yaml, USERNAME);
        if (yaml.containsKey(PASSWORD)) password = MapUtils.getStringValue(yaml, PASSWORD);
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

    int getStartDelaySeconds() {
        return startDelaySeconds;
    }

    String getUserName() {
        return userName;
    }

    String getPassword() {
        return password;
    }
}
