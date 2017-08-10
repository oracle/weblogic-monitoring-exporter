package io.prometheus.wls.rest;

import java.io.IOException;

/**
 * An abstraction of queries to the REST API.
 */
interface WebClient {

    /**
     * Initializes the client
     * @param url the URL of the WLS REST service
     */
    void initialize(String url);

    /**
     * Sets the credentials for the client.
     * @param username the user authorization required for the service
     * @param password the password required to access the service
     */
    void setCredentials(String username, String password);

    /**
     * Sets the content of the authentication header to be sent on every request
     * @param authentication authentication credentials
     */
    void setAuthenticationCredentials(String authentication);

    /**
     * Sends a query to the REST service and returns the reply.
     * @param jsonQuery a query for runtime data
     * @return a string in json format
     */
    String doQuery(String jsonQuery) throws IOException;
}
