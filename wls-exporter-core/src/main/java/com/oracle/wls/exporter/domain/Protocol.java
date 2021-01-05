// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

public enum Protocol {
  HTTP, HTTPS;

  public String format(String urlPattern, String host, int port) {
      return String.format(urlPattern, toString().toLowerCase(), host, port);
  }

  public static Protocol getProtocol(boolean secure) {
    return secure ? HTTPS : HTTP;
  }
}
