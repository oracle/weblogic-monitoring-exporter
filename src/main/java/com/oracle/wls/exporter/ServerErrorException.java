// Copyright 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

/**
 * An exception thrown when a 5xx status code is received from the server.
 */
public class ServerErrorException extends WebClientException {

  final int status;

  public ServerErrorException(int status) {
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
