// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import com.oracle.wls.exporter.domain.Protocol;

/**
 * A class which can build a URL to access the WebLogic REST API. More than one port may be specified
 * with the #withPort method. If so, a failure on the first port will cause the exporter to retry with the next
 * one, and so on. Once a connection has succeeded with a port, that port will be preferred in any future list
 * until it fails.
 */
public class UrlBuilder {

  private static class WebHost implements Comparable<WebHost> {
    private final String hostName;
    private final int port;

    public WebHost(String hostName, int port) {
      this.hostName = hostName;
      this.port = port;
    }

    @Override
    public String toString() {
      return hostName + ':' + port;
    }

    @Override
    public int compareTo(WebHost o) {
      if (hostName.equals(o.hostName)) {
        return Integer.compare(port, o.port);
      } else {
        return hostName.compareTo(o.hostName);
      }
    }

    private String format(Protocol protocol, String urlPattern) {
      return protocol.format(urlPattern, hostName, port);
    }
  }

  private static final Set<WebHost> successes = new ConcurrentSkipListSet<>();

  private final Protocol protocol;
  private final List<Integer> ports = new ArrayList<>();
  private final List<String> hostNames = new ArrayList<>();
  private final List<WebHost> failedHosts = new ArrayList<>();

  private Queue<WebHost> hosts;
  private WebHost lastCandidate;

  private UrlBuilder(boolean secure) {
    protocol = Protocol.getProtocol(secure);
  }

  /**
   * Creates a new URLBuilder.
   * @param secure true if a secure connection is to be used.
   */
  public static UrlBuilder create(boolean secure) {
    return new UrlBuilder(secure);
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

  /**
   * Modifies this URLBuilder to add a possible hostname for the WLS REST API.
   * @param hostName the hostname to try. May be null, in which case it will be ignored.
   */
  public UrlBuilder withHostName(String hostName) {
    Optional.ofNullable(hostName).ifPresent(this::addUniqueHostName);
    return this;
  }

  private void addUniqueHostName(String hostName) {
    if (!hostNames.contains(hostName)) hostNames.add(hostName);
  }

  // Creates a URL, using the specified pattern to insert the protocol, hostName and port.
  public String createUrl(String urlPattern) {
    return selectHost().format(protocol, urlPattern);
  }

  public static void clearHistory() {
    successes.clear();
  }

  private WebHost selectHost() {
    if (hosts == null) hosts = initializeHosts();
    if (hosts.isEmpty()) throw new WebClientException("No connection port is defined");

    return lastCandidate = hosts.peek();
  }

  private Queue<WebHost> initializeHosts() {
    final PriorityQueue<WebHost> hosts = new PriorityQueue<>(new PreferSuccesses());
    for (String hostName : hostNames) {
      for (int port : ports) {
        hosts.add(new WebHost(hostName, port));
      }
    }
    return hosts;
  }

  // Informs the builder that the last URL it supplied worked
  public void reportSuccess() {
    Optional.ofNullable(lastCandidate).ifPresent(successes::add);
  }

  // Informs the builder that the last URL it supplied failed.
  public void reportFailure(WebClientException connectionException) {
    successes.clear();
    failedHosts.add(lastCandidate);
    hosts.remove();
    if (hosts.isEmpty()) throw connectionException;
  }

  // Returns a collection of failed hosts.
  public List<String> getFailedHosts() {
    return failedHosts.stream().map(this::toHostString).collect(Collectors.toList());
  }

  private String toHostString(WebHost host) {
    return host.format(protocol, "%s://%s:%d");
  }

  private static final int SELECT_FIRST = -1;
  private static final int NO_PREFERENCE = 0;
  private static final int SELECT_LAST = 1;

  private class PreferSuccesses implements Comparator<WebHost> {

    @Override
    public int compare(WebHost first, WebHost second) {
      if (wasSuccessful(first) == wasSuccessful(second)) {
        return hostsFirst(first, second);
      } else if (wasSuccessful(first)) {
        return SELECT_FIRST;
      } else {
        return SELECT_LAST;
      }
    }

    private int hostsFirst(WebHost first, WebHost second) {
      int result = Integer.compare(hostNames.indexOf(first.hostName), hostNames.indexOf(second.hostName));
      return result == NO_PREFERENCE ? comparePorts(first, second) : result;
    }

    private int comparePorts(WebHost first, WebHost second) {
      return Integer.compare(ports.indexOf(first.port), ports.indexOf(second.port));
    }
  }

  private static boolean wasSuccessful(WebHost host) {
    return successes.contains(host);
  }
}
