package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * The means by which the exporter can share its configuration with other instances in a cluster.
 *
 * @author Russell Gold
 */
interface ConfigurationUpdater {

    /**
     * Returns the timestamp associated with the latest known configuration update.
     * @return a timestamp, guaranteed to be non-decreasing across subsequent calls
     */
    long getLatestConfigurationTimestamp();

    /**
     * Distribute the specified configuration, with a timestamp indicating when it was created.
     * @param configuration a yaml representation of the configuration
     */
    void shareConfiguration(String configuration);

    /**
     * Returns the latest available update.
     * @return an update containing a configuration string and associated timestamp
     */
    ConfigurationUpdate getUpdate();
}
