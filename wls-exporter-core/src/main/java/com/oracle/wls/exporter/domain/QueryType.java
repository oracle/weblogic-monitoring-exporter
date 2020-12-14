// Copyright (c) 2019, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

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
    }, CONFIGURATION {
        @Override
        public String getUrlPattern() {
            return CONFIGURATION_URL_PATTERN;
        }

        @Override
        public boolean acceptsStrings() {
            return true;
        }

        @Override
        public void processMetrics(Map<String, Object> metrics, Consumer<Map<String, String>> process) {
            Map<String,String> selected = new HashMap<>();
            Optional.ofNullable((String) metrics.remove("name")).ifPresent(n->selected.put(DOMAIN_KEY, n));

            if (!selected.isEmpty()) process.accept(selected);
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
    static final String DOMAIN_KEY = "domainName";

    /**
     * Returns the appropriate pattern to create a URL string.
     *
     * @return a pattern on which to call {@link String#format(String, Object...)}
     */
    public abstract String getUrlPattern();

    public abstract boolean acceptsStrings();

    public void processMetrics(Map<String, Object> metrics, Consumer<Map<String, String>> process) {}
}
