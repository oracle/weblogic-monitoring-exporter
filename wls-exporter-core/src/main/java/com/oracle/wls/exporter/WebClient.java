// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

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
   * Sends a PUT query to the server and returns the reply.
   * @param putBody query data
   * @return the body of the response
   */
  @SuppressWarnings("UnusedReturnValue")
  String doPutRequest(String putBody) throws IOException;

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
   * Sets the credential session cookie to be sent on each request
   * @param sessionCookie the encoded cookie
   */
  void setSessionCookie(String sessionCookie);

  /**
   * Returns the credential session cookie defined for this web client.
   */
  String getSessionCookie();

  /**
   * Returns the value of the set-cookie header from the server, indicating that a session cookie should be cached.
   */
  String getSetCookieHeader();

  /**
   * Populates the specified response with the response headers recorded in this web client,
   * sending them back to the external client. This is generally a web browser or Prometheus client.
   * @param resp the servlet response sent to the external client
   */
  void forwardResponseHeaders(HttpServletResponse resp);

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
