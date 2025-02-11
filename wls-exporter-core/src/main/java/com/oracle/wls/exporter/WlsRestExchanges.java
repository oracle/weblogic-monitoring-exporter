// Copyright (c) 2021, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * A diagnostic class that records requests sent to WLS and the replies received.
 */
public class WlsRestExchanges {

  public static final int MAX_EXCHANGES = 5;
  private static final String TEMPLATE = "At %s, REQUEST to %s:%n%s%nREPLY:%n%s%n";

  private static final Queue<Exchange> exchanges = new ConcurrentLinkedDeque<>();
  static final int TEN_MINUTES = 10;

  private static class Exchange {
    private final OffsetDateTime time;
    private final String url;
    private final String request;
    private final String response;

    Exchange(String url, String request, String response) {
      this.time = SystemClock.now();
      this.url = url;
      this.request = request;
      this.response = response;
    }

    private String getContent() {
      return String.format(TEMPLATE, ISO_LOCAL_TIME.format(time), url, request, response);
    }

    private int getContentLength() {
      return getContent().getBytes(Charset.defaultCharset()).length;
    }

    private boolean isWithinTenMinutes() {
      return Duration.between(time, SystemClock.now()).toMinutes() <= TEN_MINUTES;
    }
  }

  private WlsRestExchanges() {
  }

  /**
   * Returns a list of the most recent exchanges.
   */
  public static List<String> getExchanges() {
      return exchanges.stream().map(Exchange::getContent).collect(Collectors.toList());
  }

  /**
   * Adds an exchange for later retrieval
   * @param url the URL of the WLS instance
   * @param request the request JSON string
   * @param response the returned JSON string
   */
  public static void addExchange(String url, String request, String response) {
      exchanges.add(new Exchange(url, request, response));
      if (exchanges.size() > MAX_EXCHANGES) exchanges.remove();
  }

  /**
   * Clears the set of recorded exchanges. Intended for unit testing.
   */
  public static void clear() {
      exchanges.clear();
  }

  public static int getMessageAllocation() {
    return exchanges.stream().map(Exchange::getContentLength).reduce(Integer::sum).orElse(0);
  }

  public static int getMaximumExchangeLength() {
    return exchanges.stream().map(Exchange::getContentLength).reduce(Math::max).orElse(0);
  }

  public static int getTotalExchangeLengthOverPastTenMinutes() {
    return exchanges.stream().filter(Exchange::isWithinTenMinutes).map(Exchange::getContentLength).reduce(Integer::sum).orElse(0);
  }
}
