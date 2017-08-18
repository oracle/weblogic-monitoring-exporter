package io.prometheus.wls.rest;

public interface WebClientFactory {

    /**
     * Sets the credentials for the client.
     * @param username the user authorization required for the service
     * @param password the password required to access the service
     */
    void setCredentials(String username, String password);

    /**
     * Creates a client which will send queries to the specified URL
     * @param url the URL of the WLS REST service
     */
    WebClient createClient(String url);
}
