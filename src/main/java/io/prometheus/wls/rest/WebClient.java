package io.prometheus.wls.rest;

import java.io.IOException;

/**
 * An abstraction of queries to the REST API.
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
