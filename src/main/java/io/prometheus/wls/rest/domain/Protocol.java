package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2020, Oracle Corporation and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import javax.servlet.ServletRequest;

public enum Protocol {
  HTTP, HTTPS;

  String format(String urlPattern, String host, int port) {
      return String.format(urlPattern, toString().toLowerCase(), host, port);
  }

  public static Protocol getProtocol(ServletRequest request) {
    return request.isSecure() ? HTTPS : HTTP;
  }
}
