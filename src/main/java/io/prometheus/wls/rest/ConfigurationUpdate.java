package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * An update to the live configuration. This object is exchanged with the config coordinator server in JSON format
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

    long getTimestamp() {
        return timestamp;
    }

    String getConfiguration() {
        return configuration;
    }
}
