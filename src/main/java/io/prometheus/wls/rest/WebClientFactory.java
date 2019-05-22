package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

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
