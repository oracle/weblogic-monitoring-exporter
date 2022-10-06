// Copyright (c) 2019, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.Map;

public enum QueryType {
    RUNTIME {
        @Override
        public String getUrlPattern() {
            return RUNTIME_URL_PATTERN;
        }

        @Override
        public boolean acceptsStrings() {
            return false;
        }

        @Override
        public void postProcessMetrics(Map<String, Object> metrics, MetricsProcessor processor) {
            // do nothing
        }
    },
    CONFIGURATION {
        @Override
        public String getUrlPattern() {
            return CONFIGURATION_URL_PATTERN;
        }

        @Override
        public boolean acceptsStrings() {
            return true;
        }

        @Override
        public void postProcessMetrics(Map<String, Object> metrics, MetricsProcessor processor) {
            processor.updateConfiguration(metrics);
        }
    };


    /**
     * The pattern for a URL to which runtime REST queries are made.
     */
    public static final String RUNTIME_URL_PATTERN = "%s://%s:%d/management/weblogic/latest/serverRuntime/search";

    /**
     * The pattern for a URL to which configuration REST queries are made.
     */
    public static final String CONFIGURATION_URL_PATTERN = "%s://%s:%d/management/weblogic/latest/serverConfig/search";
    static final String DOMAIN_KEY = "name";

    /**
     * Returns the appropriate pattern to create a URL string.
     *
     * @return a pattern on which to call {@link String#format(String, Object...)}
     */
    public abstract String getUrlPattern();

    public abstract boolean acceptsStrings();

    public abstract void postProcessMetrics(Map<String, Object> metrics, MetricsProcessor processor);
}
