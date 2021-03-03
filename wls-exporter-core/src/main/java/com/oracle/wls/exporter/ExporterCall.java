// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.oracle.wls.exporter.domain.MBeanSelector;

import static com.oracle.wls.exporter.domain.MapUtils.isNullOrEmptyString;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class ExporterCall extends AuthenticatedCall {

  public ExporterCall(WebClientFactory webClientFactory, InvocationContext context) {
    super(webClientFactory, context);
    Locale.setDefault(Locale.US);
  }

  @Override
  protected void invoke(WebClient webClient, InvocationContext context) throws IOException {
    LiveConfiguration.updateConfiguration();
    try (OutputStream responseStream = context.getResponseStream();
         MetricsStream metricsStream = new MetricsStream(getInstanceName(), responseStream)) {
      if (!LiveConfiguration.hasQueries())
        metricsStream.println("# No configuration defined.");
      else {
        displayMetrics(webClient, metricsStream);
      }
    }
  }

  private void displayMetrics(WebClient webClient, MetricsStream metricsStream) throws IOException {
    try {
      for (MBeanSelector selector : LiveConfiguration.getQueries())
        displayMetrics(webClient, metricsStream, selector);
      metricsStream.printPerformanceMetrics();
    } catch (RestPortConnectionException e) {
      reportFailure(e);
      webClient.setRetryNeeded();
    }
  }

  private void displayMetrics(WebClient webClient, MetricsStream metricsStream, MBeanSelector selector) throws IOException {
    try {
      Map<String, Object> metrics = getMetrics(webClient, selector);
      if (metrics != null)
        sort(metrics).forEach(metricsStream::printMetric);
    } catch (RestQueryException e) {
      metricsStream.println(
            withCommentMarkers("REST service was unable to handle this query and returned a " + HTTP_BAD_REQUEST + "\n"
                  + selector.getPrintableRequest()));
    } catch (AuthenticationChallengeException e) {  // don't add a message for this case
      throw e;
    } catch (IOException | RuntimeException | Error e) {
      WlsRestExchanges.addExchange(getQueryUrl(selector), selector.getRequest(), e.toString());
      throw e;
    }
  }

  private static String withCommentMarkers(String string) {
    StringBuilder sb = new StringBuilder();
    for (String s : string.split("\\r?\\n"))
      sb.append("# ").append(s).append(System.lineSeparator());
    return sb.toString();
  }

  private Map<String, Object> getMetrics(WebClient webClient, MBeanSelector selector) throws IOException {
    String jsonResponse = requestMetrics(webClient, selector);
    if (isNullOrEmptyString(jsonResponse)) return null;

    return LiveConfiguration.scrapeMetrics(selector, jsonResponse);
  }

  private String requestMetrics(WebClient webClient, MBeanSelector selector) throws IOException {
    String url = getQueryUrl(selector);
    String jsonResponse = webClient.withUrl(url).doPostRequest(selector.getRequest());
    WlsRestExchanges.addExchange(url, selector.getRequest(), jsonResponse);
    return jsonResponse;
  }

  private TreeMap<String, Object> sort(Map<String, Object> metrics) {
    return new TreeMap<>(metrics);
  }
}
