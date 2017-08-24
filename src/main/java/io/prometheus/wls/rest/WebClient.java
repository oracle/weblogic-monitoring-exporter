package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import java.io.IOException;

/**
 * An abstraction of queries to the REST API.
 *
 * @author Russell Gold
 */
interface WebClient {

    /**
     * Adds a header to be sent on every request
     * @param key the header name
     * @param value the header value
     */
    void putHeader(String key, String value);

    /**
     * Sends a query to the REST service and returns the reply.
     * @param jsonQuery a query for runtime data
     * @return a string in json format
     */
    String doQuery(String jsonQuery) throws IOException;

    /**
     * Returns the set-cookie header received from the server, if any
     */
    String getSetCookieHeader();
}
