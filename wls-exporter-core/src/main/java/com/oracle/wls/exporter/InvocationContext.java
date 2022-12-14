// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Context for the invocation of AuthenticatedCall objects. This largely acts as a facade for request and response objects.
 */
public interface InvocationContext {

  /**
   * Creates an object that will generate an appropriate URL to contact WebLogic.
   */
  UrlBuilder createUrlBuilder();

  /**
   * Returns the root context of the exporter application, for use in error messages.
   */
  String getApplicationContext();

  /**
   * Returns the authentication header sent to the exporter from the client. It will be passed on to WebLogic.
   */
  String getAuthenticationHeader();

  /**
   * Returns the content type of the client request.
   */
  String getContentType();

  /**
   * Returns an identifier for the WebLogic Server instance. It will be included in generated metrics.
   */
  String getInstanceName();

  /**
   * Returns a stream from which client request contents may be read.
   * @throws IOException if unable to get the stream
   */
  InputStream getRequestStream() throws IOException;

  /**
   * Returns a stream to which responses to the client may be written.
   * @throws IOException if unable to get the stream
   */
  PrintStream getResponseStream() throws IOException;

  /**
   * Updates the response to specify an error code and explanatory message and closes the response stream.
   * @param status an HTTP error code
   * @param msg a descriptive message
   */
  void sendError(int status, String msg) throws IOException;

  /**
   *  Updates the response to redirect to a new web location and closes he response stream.
   * @param location the location, relative to the web application
   * @throws IOException if unable to redirect
   */
  void sendRedirect(String location) throws IOException;

  /**
   * Sets a header on the response.
   * @param name the header name
   * @param value the value for the header
   */
  void setResponseHeader(String name, String value);

  /**
   * Updates the response with a status but does not close the response stream.
   * @param status an HTTP status code
   */
  void setStatus(int status);

  /**
   * Closes this context and flushes any pending outputs.
   */
  void close();
}
