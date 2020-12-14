// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.Map;
/**
 * Configuration for synchronization of queries
 */
public class QuerySyncConfiguration {
    private static final String URL = "url";
    private static final String REFRESH_INTERVAL = "interval";
    private static final long DEFAULT_REFRESH_INTERVAL = 10;

    private final String url;
    private final long refreshInterval;

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
