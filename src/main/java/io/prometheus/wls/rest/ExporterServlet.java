package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

import io.prometheus.wls.rest.domain.MBeanSelector;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The servlet which produces the exported metrics.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + ServletConstants.METRICS_PAGE)
public class ExporterServlet extends PassThroughAuthenticationServlet {

    @SuppressWarnings("unused")  // production constructor
    public ExporterServlet() {
        this(new WebClientFactoryImpl());
    }

    ExporterServlet(WebClientFactory webClientFactory) {
        super(webClientFactory);
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        LiveConfiguration.init(servletConfig);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doWithAuthentication(req, resp, this::displayMetrics);
    }

    @SuppressWarnings("unused") // The req parameter is not used, but is required by 'doWithAuthentication'
    private void displayMetrics(WebClient webClient, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LiveConfiguration.updateConfiguration();
        try (MetricsStream metricsStream = new MetricsStream(resp.getOutputStream())) {
            if (!LiveConfiguration.hasQueries())
                metricsStream.println("# No configuration defined.");
            else
                printMetrics(webClient, metricsStream);

        }
    }

    private void printMetrics(WebClient webClient, MetricsStream metricsStream) throws IOException {
        for (MBeanSelector selector : LiveConfiguration.getQueries())
            displayMetrics(webClient, metricsStream, selector);
        metricsStream.printPerformanceMetrics();
    }

    private void displayMetrics(WebClient webClient, MetricsStream metricsStream, MBeanSelector selector) throws IOException {
        try {
            Map<String, Object> metrics = getMetrics(webClient, selector);
            if (metrics != null)
                sort(metrics).forEach(metricsStream::printMetric);
        } catch (RestQueryException e) {
            metricsStream.println(
                  withCommentMarkers("REST service was unable to handle this query\n"
                      + "exception: " + e.getMessage() + '\n'
                      + selector.getPrintableRequest()));
        }
    }

    private String withCommentMarkers(String string) {
        StringBuilder sb = new StringBuilder();
        for (String s : string.split("\\r?\\n"))
            sb.append("# ").append(s).append(System.lineSeparator());
        return sb.toString();
    }

    private TreeMap<String, Object> sort(Map<String, Object> metrics) {
        return new TreeMap<>(metrics);
    }

    private Map<String, Object> getMetrics(WebClient webClient, MBeanSelector selector) throws IOException {
        String request = selector.getRequest();
        String jsonResponse = webClient.withUrl(LiveConfiguration.getUrl(selector)).doPostRequest(request);
        MessagesServlet.addExchange(request, jsonResponse);
        if (isNullOrEmptyString(jsonResponse)) return null;

        return LiveConfiguration.scrapeMetrics(selector, jsonResponse);
    }

}
