package io.prometheus.wls.rest;

import io.prometheus.wls.rest.domain.ExporterConfig;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

class LiveConfiguration {
    private static ExporterConfig config;

    static {
        loadFromString("");
    }

    static ExporterConfig getConfig() {
        return config;
    }

    static void setConfig(ExporterConfig config) {
        LiveConfiguration.config = config;
    }


    @SuppressWarnings("unchecked")
    static void loadFromString(String yamlString) {
        Map<String, Object> yamlConfig = (Map<String, Object>) new Yaml().load(yamlString);

        config = ExporterConfig.loadConfig(yamlConfig);
    }

    static boolean hasQueries() {
        return getConfig() != null && getConfig().getQueries().length > 0;
    }
}
