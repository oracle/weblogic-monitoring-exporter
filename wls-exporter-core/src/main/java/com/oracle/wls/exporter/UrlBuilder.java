// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import com.oracle.wls.exporter.domain.Protocol;

/**
 * A class which can build a URL to access the WebLogic REST API. More than one port may be specified
 * with the #withPort method. If so, a failure on the first port will cause the exporter to retry with the next
 * one, and so on. Once a connection has succeeded with a port, that port will be preferred in any future list
 * until it fails.
 */
public class UrlBuilder {

  private static final Set<Integer> successes = new ConcurrentSkipListSet<>();

  private final String host;
  private final Protocol protocol;
  private final Queue<Integer> ports = new PriorityQueue<>(new PreferSuccesses());

  private int lastCandidate;

  private UrlBuilder(String hostName, boolean secure) {
    host = hostName;
    protocol = Protocol.getProtocol(secure);
  }

  /**
   * Creates a new URLBuilder.
   * @param hostName the host for the WLS instance
   * @param secure true if a secure connection is to be used.
   */
  public static UrlBuilder create(String hostName, boolean secure) {
    return new UrlBuilder(hostName, secure);
  }

  /**
   * Modifies this URLBuilder to add a possible port for the WLS REST API.
   * @param port the port to try. May be null, in which case it will be ignored.
   */
  public UrlBuilder withPort(Integer port) {
    Optional.ofNullable(port).ifPresent(this::addUniquePort);
    return this;
  }

  private void addUniquePort(int port) {
    if (!ports.contains(port)) ports.add(port);
  }

  // Creates a URL, using the specified pattern to insert the protocol, host and port.
  public String createUrl(String urlPattern) {
    return protocol.format(urlPattern, host, selectPort());
  }

  public static void clearHistory() {
    successes.clear();
  }

  private int selectPort() {
    if (ports.isEmpty()) throw new WebClientException("No connection port is defined");

    return lastCandidate = ports.peek();
  }

  // Informs the builder that the last URL it supplied worked
  public void reportSuccess() {
    successes.add(lastCandidate);
  }

  // Informs the builder that the last URL it supplied failed.
  public void reportFailure(WebClientException connectionException) {
    successes.clear();
    ports.remove();
    if (ports.isEmpty()) throw connectionException;
  }

  private static final int SELECT_FIRST = -1;
  private static final int NO_PREFERENCE = 0;
  private static final int SELECT_LAST = +1;
  private static class PreferSuccesses implements Comparator<Integer> {

    @Override
    public int compare(Integer first, Integer last) {
      if (wasSuccessful(first) == wasSuccessful(last)) {
        return NO_PREFERENCE;
      } else if (wasSuccessful(first)) {
        return SELECT_FIRST;
      } else {
        return SELECT_LAST;
      }
    }
  }

  private static boolean wasSuccessful(Integer port) {
    return successes.contains(port);
  }
}
