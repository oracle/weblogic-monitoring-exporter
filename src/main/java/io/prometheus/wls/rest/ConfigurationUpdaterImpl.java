package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import com.google.gson.Gson;
import io.prometheus.wls.rest.domain.QuerySyncConfiguration;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

/**
 * An object to manage interactions with the configuration repeater over HTTP.
 *
 * @author Russell Gold
 */
class ConfigurationUpdaterImpl implements ConfigurationUpdater {
    private WebClientFactory factory;
    private Clock clock;
    private ConfigurationUpdate latest;
    private String repeaterUrl;
    private long refreshInterval;
    private Instant nextUpdateTime;
    private ErrorLog errorLog = new ErrorLog();

    /**
     * Creates the updater.
     * @param syncConfiguration the configuration to apply to the updater
     * @param errorLog a log to which errors should be reported
     */
    ConfigurationUpdaterImpl(QuerySyncConfiguration syncConfiguration, ErrorLog errorLog) {
        this(Clock.systemUTC(), new WebClientFactoryImpl());
        this.errorLog = errorLog;
        configure(syncConfiguration.getUrl(), syncConfiguration.getRefreshInterval());
    }

    /**
     * Creates a version of the updater to which delegates can be specified. Primarily used for unit testing.
     * @param clock the clock indicating the current time
     * @param factory a factory for web clients
     */
    ConfigurationUpdaterImpl(Clock clock, WebClientFactory factory) {
        this.clock = clock;
        this.factory = factory;
    }

    /**
     * Defines an error log to track problems with configuration updates.
     * @param errorLog the new log
     */
    void setErrorLog(ErrorLog errorLog) {
        this.errorLog = errorLog;
    }

    /**
     * Defines the configuration for this updater
     * @param repeaterUrl the url to contact to share and retrieve updates
     * @param refreshInterval the interval, in seconds, to wait before checking for an update
     */
    void configure(String repeaterUrl, long refreshInterval) {
        this.repeaterUrl = repeaterUrl;
        this.refreshInterval = refreshInterval;
    }

    @Override
    public long getLatestConfigurationTimestamp() {
        getLatestConfiguration();
        return latest == null ? 0 : latest.getTimestamp();
    }

    private void getLatestConfiguration() {
        if (nextUpdateTime != null && nextUpdateTime.isAfter(clock.instant())) return;
        try {
            WebClient client = factory.createClient().withUrl(repeaterUrl);
            latest = new Gson().fromJson(client.doGetRequest(), ConfigurationUpdate.class);
            nextUpdateTime = clock.instant().plusSeconds(refreshInterval);
        } catch (IOException | WebClientException e) {
            errorLog.log(e);
            latest = null;
        }
    }

    @Override
    public void shareConfiguration(String configuration) {
        try {
            WebClient client = factory.createClient().withUrl(repeaterUrl);

            client.doPutRequest(new Gson().toJson(createUpdate(configuration)));
        } catch (IOException | WebClientException e) {
            errorLog.log(e);
        }
    }

    private ConfigurationUpdate createUpdate(String configuration) {
        return new ConfigurationUpdate(clock.instant().toEpochMilli(), configuration);
    }

    @Override
    public ConfigurationUpdate getUpdate() {
        getLatestConfiguration();
        return latest;
    }

    String getRepeaterUrl() {
        return repeaterUrl;
    }

    long getRefreshInterval() {
        return refreshInterval;
    }
}
