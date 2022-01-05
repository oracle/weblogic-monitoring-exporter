// Copyright 2020, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.Optional;

/**
 * An exception thrown when a 5xx status code is received from the server.
 */
public class ServerErrorException extends WebClientException {

  final int status;

  public ServerErrorException(int status) {
    this.status = status;
  }

  public ServerErrorException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  @Override
  public String getMessage() {
    StringBuilder sb = new StringBuilder("Received status ").append(status);
    Optional.ofNullable(super.getMessage()).ifPresent(m -> sb.append(" - ").append(m));
    return sb.toString();
  }
}
