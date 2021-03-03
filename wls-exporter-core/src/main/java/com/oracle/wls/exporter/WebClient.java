// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;

/**
 * An interface for an object responsible for sending requests from exporter web application to
 * web servers.
 *
 * @author Russell Gold
 */
public interface WebClient {

  String APPLICATION_JSON = "application/json; charset=UTF-8";
  String X_REQUESTED_BY_HEADER = "X-Requested-By";

  /**
   * Sets the url to which this client will send requests.
   * @param url a URL for requests
   * @return this instance
   */
  WebClient withUrl(String url);

  /**
   * Sends a plain GET request to the defined URL without parameters
   * @return the body of the response
   */
  String doGetRequest() throws IOException;

  /**
   * Sends a POST query to the server and returns the reply.
   * @param postBody query data
   * @return the body of the response
   */
  String doPostRequest(String postBody) throws IOException;

  /**
   * Converts the specified object to JSON and uses a PUT request to send it to the server.
   * @param putBody query data
   * @return the reply
   */
  @SuppressWarnings("UnusedReturnValue")
  <T> String doPutRequest(T putBody) throws IOException;

  /**
   * Adds a header to be sent on every query.
   * @param name the header name
   * @param value the header value
   */
  void addHeader(String name, String value);

  /**
   * Sets the user credentials to be sent to the server.
   * @param authentication the encoded user credentials
   */
  void setAuthentication(String authentication);

  /**
   * Returns the user credentials defined for this web client.
   */
  String getAuthentication();

  /**
   * Set a flag to indicate that a retry of the request will be needed. Typically this means that the previous
   * request did not reach a server, and new port should be tried.
   */
  void setRetryNeeded();

  /**
   * Returns true if this client has been marked to retry the last request.
   */
  boolean isRetryNeeded();
}
