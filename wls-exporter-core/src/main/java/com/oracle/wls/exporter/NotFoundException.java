// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

/**
 * An exception indicating that the client could not connect to the specified URL.
 */
public class NotFoundException extends WebClientException {

    private final String uri;

    public NotFoundException(String uri) {
        this.uri = uri;
    }

    String getUri() {
        return uri;
    }
}
