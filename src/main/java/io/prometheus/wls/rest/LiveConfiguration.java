package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.prometheus.wls.rest.domain.ExporterConfig;
import io.prometheus.wls.rest.domain.MBeanSelector;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Russell Gold
 */
class LiveConfiguration {
    static final String CONFIG_YML = "/config.yml";
    private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
    private static ExporterConfig config;
    private static String serverName;
    private static int serverPort;
    private static ConfigurationUpdater updater = new NullConfigurationUpdater();

    static {
        loadFromString("");
    }

    private static Long timestamp;

    private static ExporterConfig getConfig() {
        return config;
    }


    @SuppressWarnings("unchecked")
    static void loadFromString(String yamlString) {
        Map<String, Object> yamlConfig = (Map<String, Object>) new Yaml().load(yamlString);

        config = ExporterConfig.loadConfig(yamlConfig);
    }

    static void setServer(String serverName, int serverPort) {
        LiveConfiguration.serverName = serverName;
        LiveConfiguration.serverPort = serverPort;
    }

    static String getQueryUrl() {
        return String.format(URL_PATTERN, serverName, serverPort);
    }

    static String getPerformanceQualifier() {
        return String.format("{instance=\"%s:%d\"}", serverName, serverPort);
    }

    static boolean hasQueries() {
        return getConfig() != null && getConfig().getQueries().length > 0;
    }

    static MBeanSelector[] getQueries() {
        return getConfig().getQueries();
    }

    static void init(ServletConfig servletConfig) {
        if (timestamp != null) return;
        
        InputStream configurationFile = getConfigurationFile(servletConfig);
        if (configurationFile != null)
            initialize(configurationFile);
    }

    private static void initialize(InputStream configurationFile) {
        config = ExporterConfig.loadConfig(configurationFile);
        timestamp = 0L;
    }

    private static InputStream getConfigurationFile(ServletConfig config) {
        return config.getServletContext().getResourceAsStream(CONFIG_YML);
    }

    static String asString() {
        return "host: " + serverName + '\n' +
               "port: " + serverPort + '\n' + getConfig();
    }

    static Map<String, Object> scrapeMetrics(MBeanSelector selector, String jsonResponse) {
        return getConfig().scrapeMetrics(selector, toJsonObject(jsonResponse));
    }

    private static JsonObject toJsonObject(String response) {
        return new JsonParser().parse(response).getAsJsonObject();
    }

    static void appendConfiguration(ExporterConfig uploadedConfig) throws ServletException {
        if (uploadedConfig == null) throw new ServletException("No configuration specified");
        getConfig().append(uploadedConfig);
        shareConfiguration();
    }

    private static void shareConfiguration() {
        updater.shareConfiguration(asString());
        timestamp = updater.getLatestConfigurationTimestamp();
    }

    static void replaceConfiguration(ExporterConfig uploadedConfig) throws ServletException {
        if (uploadedConfig == null) throw new ServletException("No configuration specified");
        getConfig().replace(uploadedConfig);
        shareConfiguration();
    }

    static long getTimestamp() {
        return timestamp;
    }

    /**
     * If a newer shared configuration is available, update it now.
     */
    static void updateConfiguration() {
        if (timestamp == null) timestamp = 0L;
        if (updater.getLatestConfigurationTimestamp() > timestamp)
            installNewConfiguration(updater.getUpdate());
    }

    private synchronized static void installNewConfiguration(ConfigurationUpdate update) {
        if (update.getTimestamp() > timestamp) {
            getConfig().replace(toConfiguration(update.getConfiguration()));
            timestamp = update.getTimestamp();
        }
    }

    private static ExporterConfig toConfiguration(String configuration) {
        return ExporterConfig.loadConfig(new ByteArrayInputStream(configuration.getBytes()));
    }

    static class NullConfigurationUpdater implements ConfigurationUpdater {
        @Override
        public long getLatestConfigurationTimestamp() {
            return 0;
        }

        @Override
        public void shareConfiguration(String configuration) {

        }

        @Override
        public ConfigurationUpdate getUpdate() {
            return null;
        }
    }
}
