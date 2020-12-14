// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.servlet.http.HttpServletRequest;

import com.oracle.wls.exporter.domain.Protocol;

public class UrlBuilder {

  private static final Set<Integer> successes = new ConcurrentSkipListSet<>();

  private final String host;
  private final Protocol protocol;
  private final Queue<Integer> ports = new PriorityQueue<>(new PreferSuccesses());
  private int lastCandidate;

  public UrlBuilder(HttpServletRequest request, Integer restPort) {
    host = request.getLocalName();
    protocol = Protocol.getProtocol(request);
    Optional.ofNullable(restPort).ifPresent(ports::add);
    addPortIfNotPresent(request.getLocalPort());
  }

  private void addPortIfNotPresent(int port) {
    if (!ports.contains(port)) ports.add(port);
  }

  public static void clearHistory() {
    successes.clear();
  }

  public String createUrl(String urlPattern) {
    return protocol.format(urlPattern, host, selectPort());
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
