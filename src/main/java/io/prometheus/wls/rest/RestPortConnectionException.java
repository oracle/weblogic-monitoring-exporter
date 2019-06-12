// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package io.prometheus.wls.rest;

import org.apache.http.HttpHost;

/**
 * An exception indicating that the client could not connect to the REST port.
 */
class RestPortConnectionException extends WebClientException {

    private final String uri;

    RestPortConnectionException(HttpHost host) {
        uri = host.toURI();
    }

    String getUri() {
        return uri;
    }
}
