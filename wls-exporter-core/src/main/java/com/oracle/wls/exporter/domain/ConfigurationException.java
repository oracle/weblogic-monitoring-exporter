// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * An exception thrown if there is a problem with the exporter configuration.
 *
 * @author Russell Gold
 */
public class ConfigurationException extends RuntimeException {
    public static final String BAD_YAML_FORMAT = "Configuration YAML format has errors";
    public static final String NOT_YAML_FORMAT = "Configuration is not in YAML format";
    static final String NO_QUERY_SYNC_URL = "query_sync defined without url";
    private final List<String> context = new ArrayList<>();

    ConfigurationException(String description) {
        super(description);
    }

    void addContext(String parentContext) {
        context.add(0, parentContext);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (!context.isEmpty())
            sb.append(" at ").append(String.join(".", context));
        return sb.toString();
    }
}
