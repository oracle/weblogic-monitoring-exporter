// Copyright (c) 2021, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * A diagnostic class that records requests sent to WLS and the replies received.
 */
public class WlsRestExchanges {

  public static final int MAX_EXCHANGES = 5;
  private static final String TEMPLATE = "At %s, REQUEST to %s:%n%s%nREPLY:%n%s%n";

  private static final Queue<String> exchanges = new ConcurrentLinkedDeque<>();

  private WlsRestExchanges() {
  }

  /**
   * Returns a list of the most recent exchanges.
   */
  public static List<String> getExchanges() {
      return new ArrayList<>(exchanges);
  }

  /**
   * Adds an exchange for later retrieval
   * @param url the URL of the WLS instance
   * @param request the request JSON string
   * @param response the returned JSON string
   */
  public static void addExchange(String url, String request, String response) {
      exchanges.add(String.format(TEMPLATE, getCurrentTime(), url, request, response));
      if (exchanges.size() > MAX_EXCHANGES) exchanges.remove();
  }

  private static String getCurrentTime() {
    return ISO_LOCAL_TIME.format(SystemClock.now());
  }

  /**
   * Clears the set of recorded exchanges. Intended for unit testing.
   */
  public static void clear() {
      exchanges.clear();
  }
}
