// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

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
