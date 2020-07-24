package com.oracle.wls.exporter;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
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
