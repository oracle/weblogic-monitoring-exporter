// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

/**
 * An interface for creating web clients.
 *
 * @author Russell Gold
 */
public interface WebClientFactory {

    /**
     * Creates a client which will send queries to a specified URL
     */
    WebClient createClient();
}
