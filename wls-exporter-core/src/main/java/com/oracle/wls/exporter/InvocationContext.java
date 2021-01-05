// Copyright (c) 2021, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public interface InvocationContext {

  UrlBuilder createUrlBuilder();

  InputStream getRequestStream() throws IOException;

  String getContentType();

  String getInstanceName();

  String getAuthenticationHeader();

  PrintStream getResponseStream() throws IOException;

  String getApplicationContext();

  void close();

  void sendRedirect(String location) throws IOException;

  void sendError(int status) throws IOException;

  void sendError(int status, String msg) throws IOException;

  void setStatus(int status);

  void setHeader(String name, String value);
}
