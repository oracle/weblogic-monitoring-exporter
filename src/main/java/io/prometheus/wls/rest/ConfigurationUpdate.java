package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * An update to the live configuration
 *
 * @author Russell Gold
 */
class ConfigurationUpdate {
    private long timestamp;
    private String configuration;

    ConfigurationUpdate(long timestamp, String configuration) {
        this.timestamp = timestamp;
        this.configuration = configuration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getConfiguration() {
        return configuration;
    }
}
