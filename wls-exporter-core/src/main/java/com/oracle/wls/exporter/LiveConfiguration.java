// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oracle.wls.exporter.domain.ExporterConfig;
import com.oracle.wls.exporter.domain.MBeanSelector;
import com.oracle.wls.exporter.domain.QuerySyncConfiguration;
import org.yaml.snakeyaml.Yaml;

/**
 * The repository for the current exporter configuration.
 *
 * @author Russell Gold
 */
class LiveConfiguration {
    /** The path to the configuration file within the web application. */
    static final String CONFIG_YML = "/config.yml";

    /** The address used to access WLS (cannot use the address found in the request due to potential server-side request forgery. */
    static final String WLS_HOST;
    
    private static ExporterConfig config;
    private static String serverName;
    private static int serverPort;
    private static ConfigurationUpdater updater = new NullConfigurationUpdater();
    private static ErrorLog errorLog = new ErrorLog();

    static {
        loadFromString("");
        WLS_HOST = getLocalHostName();
    }

    private static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    private static Long timestamp;

    private static ExporterConfig getConfig() {
        return config;
    }


    static void loadFromString(String yamlString) {
        Map<String, Object> yamlConfig = new Yaml().load(yamlString);

        config = ExporterConfig.loadConfig(yamlConfig);
    }

    /**
     * Specifies the server on which to contact the Management RESTful services.
     *
     * @param serverName the name of the server
     * @param serverPort the port on which the server is listening
     */
    static void setServer(String serverName, int serverPort) {
        LiveConfiguration.serverName = serverName;
        LiveConfiguration.serverPort = serverPort;
    }

    /**
     * Specifies the server on which to contact the Management RESTful services.
     *
     * @param req the incoming request
     */
    static void setServer(HttpServletRequest req) {
        LiveConfiguration.setServer(req.getServerName(), req.getServerPort());
    }

    /**
     * Creates a builder for URLs that can handle retries to alternative ports.
     * @param request the active servlet request
     * @return the new builder
     */
    static UrlBuilder createUrlBuilder(HttpServletRequest request) {
        return new UrlBuilder(request, getConfiguredRestPort());
    }

    static Integer getConfiguredRestPort() {
        return Optional.ofNullable(config).map(ExporterConfig::getRestPort).orElse(null);
    }

    /**
     * Returns true if the live configuration has at least one query defined
     * @return a boolean which can be used to decide whether to perform a query
     */
    static boolean hasQueries() {
        return getConfig() != null && getConfig().getQueries().length > 0;
    }

    /**
     * Returns the defined queries as top-level selector objects.
     * @return an array of hierarchical mbean queries
     */
    static MBeanSelector[] getQueries() {
        return getConfig().getEffectiveQueries();
    }

    /**
     * Returns the accumulatedLoggedErrors
     * @return a string containing errors or the empty string;
     */
    static String getErrors() {
        return errorLog.getErrors();
    }

    /**
     * Loads the initial configuration during servlet load. Will skip the initialization if the configuration
     * has already been loaded from the config coordinator.
     *
     * @param servletConfig a standard servlet configuration which points to an exporter configuration
     */
    static void init(ServletConfig servletConfig) {
        if (timestamp != null) return;
        
        InputStream configurationFile = getConfigurationFile(servletConfig);
        initialize(Optional.ofNullable(configurationFile)
                .map(ExporterConfig::loadConfig)
                .orElse(ExporterConfig.createEmptyConfig()));
    }

    private static void initialize(ExporterConfig config) {
        LiveConfiguration.config = config;
        installUpdater(LiveConfiguration.config.getQuerySyncConfiguration());
        timestamp = 0L;
    }

    private static void installUpdater(QuerySyncConfiguration syncConfiguration) {
        if (syncConfiguration == null) return;

        errorLog = new ErrorLog();
        updater = new ConfigurationUpdaterImpl(syncConfiguration, errorLog);
    }

    private static InputStream getConfigurationFile(ServletConfig config) {
        return config.getServletContext().getResourceAsStream(CONFIG_YML);
    }

    /**
     * Returns a string representation of the current configuration, prepended with the server location.
     * @return a human readable representation of the configuration
     */
    static String asString() {
        return "host: " + serverName + '\n' +
               "port: " + serverPort + '\n' + getConfig();
    }

    /**
     * Converts a JSON response from the Management RESTful service to Prometheus metrics.
     * @param selector an MBean selector describing the metrics to extract
     * @param jsonResponse an object describing the current values of the desired MBean fields
     * @return a map of metric names to values
     */
    static Map<String, Object> scrapeMetrics(MBeanSelector selector, String jsonResponse) {
        return getConfig().scrapeMetrics(selector, toJsonObject(jsonResponse));
    }

    private static JsonObject toJsonObject(String response) {
        return new JsonParser().parse(response).getAsJsonObject();
    }

    /**
     * Updates the current configuration by appending any queries from the specified one to it.
     * Other fields from the new configuration, including the query_spec, will be ignored.
     *
     * @param uploadedConfig an exporter configuration
     * @throws ServletException if an error occurs while trying to perform the update.
     */
    static void appendConfiguration(ExporterConfig uploadedConfig) throws ServletException {
        if (uploadedConfig == null) throw new ServletException("No configuration specified");
        getConfig().append(uploadedConfig);
        shareConfiguration();
    }

    private static void shareConfiguration() {
        updater.shareConfiguration(asString());
        timestamp = updater.getLatestConfigurationTimestamp();
    }

    /**
     * Updates the current configuration by replacing all of its queries with those from the specified configuration.
     * Other fields from the new configuration, including the query_spec, will be ignored.
     *
     * @param uploadedConfig an exporter configuration
     * @throws ServletException if an error occurs while trying to perform the update.
     */
    static void replaceConfiguration(ExporterConfig uploadedConfig) throws ServletException {
        if (uploadedConfig == null) throw new ServletException("No configuration specified");
        getConfig().replace(uploadedConfig);
        shareConfiguration();
    }

    /**
     * Returns the timestamp of the current configuration.
     * @return a time in milliseconds, since the epoch
     */
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

    /**
     * A no-op updater used if the original configuration did not specify one.
     */
    static class NullConfigurationUpdater implements ConfigurationUpdater {
        @Override
        public long getLatestConfigurationTimestamp() {
            return 0;
        }

        @Override
        public void shareConfiguration(String configuration) {}

        @Override
        public ConfigurationUpdate getUpdate() {
            return null;
        }
    }
}
