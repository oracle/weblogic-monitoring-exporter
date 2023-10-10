// Copyright (c) 2020, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

public enum Protocol {
  HTTP, HTTPS;

  public String format(String urlPattern, String hostName, int port) {
    if (hostName.contains(":")) {
      hostName = "[" + hostName + "]";
    }
    return String.format(urlPattern, toString().toLowerCase(), hostName, port);
  }

  public static Protocol getProtocol(boolean secure) {
    return secure ? HTTPS : HTTP;
  }
}
