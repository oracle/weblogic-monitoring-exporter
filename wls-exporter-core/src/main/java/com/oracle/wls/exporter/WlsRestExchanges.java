// Copyright (c) 2021, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * A diagnostic class that records requests sent to WLS and the replies received.
 */
public class WlsRestExchanges {

  public static final int MAX_EXCHANGES = 50;
  private static final DateTimeFormatter INSTANT_FORMATTER
          = DateTimeFormatter.ofPattern("kk:mm:ss.SSS zzz").withZone(ZoneId.of("UTC"));

  private static final String FULL_TEMPLATE = "At %s, REQUEST to %s:%n%s%nREPLY at %s:%n%s%n";
  private static final String PARTIAL_TEMPLATE = "At %s, REQUEST to %s:%n%s%n";

  private static final Queue<WlsRestExchange> exchanges = new ConcurrentLinkedDeque<>();
  private static final Clock clock = Clock.systemUTC();

  private WlsRestExchanges() {
  }

  /**
   * Returns a list of the most recent exchanges.
   */
  public static List<String> getExchanges() {
    return exchanges.stream().map(WlsRestExchange::toString).collect(Collectors.toList());
  }

  /**
   * Adds an exchange for later retrieval
   * @param url the URL of the WLS instance
   * @param request the request JSON string
   * @param response the returned JSON string
   */
  public static void addExchange(String url, String request, String response) {
    WlsRestExchange wlsRestExchange = addExchange(url, request);
    wlsRestExchange.complete(response);
  }

  /**
   * Adds an exchange for later retrieval
   * @param url the URL of the WLS instance
   * @param request the request JSON string
   */
  static WlsRestExchange addExchange(String url, String request) {
    if (exchanges.size() >= MAX_EXCHANGES) exchanges.remove();

    WlsRestExchange exchange = new WlsRestExchange(url, request);
    exchanges.add(exchange);
    return exchange;
  }

  /**
   * Clears the set of recorded exchanges. Intended for unit testing.
   */
  public static void clear() {
      exchanges.clear();
  }

  static public class WlsRestExchange {
    private final String url;
    private final String request;
    private final Instant requestTime;
    private String response;
    private Instant responseTime;

    WlsRestExchange(String url, String request) {
      this.url = url;
      this.request = request;
      this.requestTime = clock.instant();
    }

    public void complete(String response) {
      this.response = response;
      this.responseTime = clock.instant();
    }

    @Override
    public String toString() {
      if (responseTime != null) {
        return String.format(FULL_TEMPLATE, toString(requestTime), url, request, toString(responseTime), response);
      } else {
        return String.format(PARTIAL_TEMPLATE, toString(requestTime), url, request);
      }
    }

    private static String toString(Instant instant) {
      return INSTANT_FORMATTER.format(instant);
    }
  }
}
