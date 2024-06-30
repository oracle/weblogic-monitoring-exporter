// Copyright (c) 2021, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oracle.wls.exporter.domain.MBeanSelector;

import static com.oracle.wls.exporter.domain.MapUtils.isNullOrEmptyString;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class ExporterCall extends AuthenticatedCall {

  private static final Semaphore KEY_SEMAPHORE = new Semaphore(1);

  private ExporterQuery exporterQuery;

  public ExporterCall(WebClientFactory webClientFactory, InvocationContext context) {
    super(webClientFactory, context);
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
      metricsStream.printPlatformMetrics();
    } catch (RestPortConnectionException e) {
      reportFailure(e);
      webClient.setRetryNeeded();
    }
  }

  private void displayMetrics(WebClient webClient, MetricsStream metricsStream, MBeanSelector selector) throws IOException {
    try {
      Map<String, Object> metrics = getMetrics(webClient, selector);
      if (!metrics.isEmpty())
        sort(metrics).forEach(metricsStream::printMetric);
    } catch (RestQueryException e) {
      reportProblem(metricsStream, selector);
    } catch (AuthenticationChallengeException e) {  // don't add a message for this case
      throw e;
    } catch (IOException | RuntimeException e) {
      WlsRestExchanges.addExchange(getQueryUrl(selector), selector.getRequest(), e.toString());
      throw e;
    }
  }

  private void reportProblem(MetricsStream metricsStream, MBeanSelector selector) {
    metricsStream.println(withCommentMarkers(getProblem(selector) + "\n" + selector.getPrintableRequest()));
  }

  private String getProblem(MBeanSelector selector) {
    if (selector.isRequestForPrivilegedProperty())
      return "You seem to have encountered a bug in the WebLogic REST API.\n" +
            " The JDBCServiceRuntime.JDBCDataSourceRuntimeMBeans.properties property " +
            " may only be accessed by a user with administrator privileges.";
    else
      return "REST service was unable to handle this query and returned a " + HTTP_BAD_REQUEST;
  }

  private static String withCommentMarkers(String string) {
    StringBuilder sb = new StringBuilder();
    for (String s : string.split("\\r?\\n"))
      sb.append("# ").append(s).append(System.lineSeparator());
    return sb.toString();
  }

  private Map<String, Object> getMetrics(WebClient webClient, MBeanSelector selector) throws IOException {
    String jsonResponse = requestMetrics(webClient, selector);
    if (isNullOrEmptyString(jsonResponse)) return Collections.emptyMap();
    return LiveConfiguration.scrapeMetrics(selector, jsonResponse);
  }

  private String requestMetrics(WebClient webClient, MBeanSelector selector) throws IOException {
    refreshKeysIfNeeded(webClient, selector);

    try {
      Optional.ofNullable(exporterQuery).ifPresent(ExporterQuery::makingRestCall);
      final String url = getQueryUrl(selector);
      final String jsonResponse = webClient.withUrl(url).doPostRequest(selector.getRequest());
      WlsRestExchanges.addExchange(url, selector.getRequest(), jsonResponse);
      Optional.ofNullable(exporterQuery).ifPresent(ExporterQuery::restCallReceived);
      return jsonResponse;
    } catch (Exception e) {
      Optional.ofNullable(exporterQuery).ifPresent(q -> q.restCallAborted(e));
      throw e;
    }
  }

  private void refreshKeysIfNeeded(WebClient webClient, MBeanSelector selector) throws IOException {
    if (!selector.needsNewKeys()) return;

    try {
      if (KEY_SEMAPHORE.tryAcquire(2, TimeUnit.SECONDS)) {
          if (!selector.needsNewKeys()) KEY_SEMAPHORE.release();
          else refreshKeys(webClient, selector);
      }
    } catch (InterruptedException ignore) {
    }
  }

  private void refreshKeys(WebClient webClient, MBeanSelector selector) throws IOException {
    try {
      Optional.ofNullable(exporterQuery).ifPresent(ExporterQuery::makingRestCallForKeys);
      final String url = getQueryUrl(selector);
      final String keyResponse = webClient.withUrl(url).doPostRequest(selector.getKeyRequest());
      Optional.ofNullable(exporterQuery).ifPresent(ExporterQuery::restCallReceived);
      WlsRestExchanges.addExchange(url, selector.getKeyRequest(), keyResponse);
      selector.offerKeys(toJsonObject(keyResponse));
    } catch (IOException e) {
      Optional.ofNullable(exporterQuery).ifPresent(q -> q.restCallAborted(e));
      throw e;
    } finally {
      KEY_SEMAPHORE.release();
    }
  }

  private static JsonObject toJsonObject(String response) {
      return JsonParser.parseString(response).getAsJsonObject();
  }

  private TreeMap<String, Object> sort(Map<String, Object> metrics) {
    return new TreeMap<>(metrics);
  }

  public void setExporterQuery(ExporterQuery exporterQuery) {
    this.exporterQuery = exporterQuery;
  }
}
