package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import java.util.Map;
/**
 * Configuration for synchronization of queries
 */
public class QuerySyncConfiguration {
    private static final String URL = "url";
    private static final String REFRESH_INTERVAL = "interval";
    private static final long DEFAULT_REFRESH_INTERVAL = 10;

    private String url;
    private long refreshInterval;

    QuerySyncConfiguration(Map<String, Object> map) {
        if (map == null || !map.containsKey(URL))
            throw new ConfigurationException(ConfigurationException.NO_QUERY_SYNC_URL);

        url = MapUtils.getStringValue(map, URL);
        refreshInterval = map.containsKey(REFRESH_INTERVAL)
                ? MapUtils.getIntegerValue(map, REFRESH_INTERVAL)
                : DEFAULT_REFRESH_INTERVAL;
    }

    public String getUrl() {
        return url;
    }

    public long getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public String toString() {
        return "query_sync:\n" +
                "  url: " + url + '\n' +
                "  refreshInterval: " + refreshInterval + '\n';
    }
}
