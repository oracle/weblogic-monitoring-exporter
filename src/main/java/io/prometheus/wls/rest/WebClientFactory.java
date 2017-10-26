package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
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
     * Creates a client which will send queries to the specified URL
     * @param url the URL of the WLS REST service
     */
    WebClient createClient(String url);
}
