// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.domain.MBeanSelector;

import static com.oracle.wls.exporter.domain.MapUtils.isNullOrEmptyString;

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
    public void init(ServletConfig servletConfig) {
        LiveConfiguration.init(servletConfig);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doWithAuthentication(req, resp, this::displayMetrics);
    }

    @SuppressWarnings("unused") // The req parameter is not used, but is required by 'doWithAuthentication'
    private void displayMetrics(WebClient webClient, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LiveConfiguration.updateConfiguration();
        try (MetricsStream metricsStream = new MetricsStream(req, resp.getOutputStream())) {
            if (!LiveConfiguration.hasQueries())
                metricsStream.println("# No configuration defined.");
            else
                printMetrics(webClient, metricsStream);
        }
    }

    private void printMetrics(WebClient webClient, MetricsStream metricsStream) throws IOException {
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
                  withCommentMarkers("REST service was unable to handle this query\n"
                        + selector.getPrintableRequest() + '\n'
                        + "exception: " + e.getMessage()));
        } catch (AuthenticationChallengeException e) {  // don't add a message for this case
            throw e;
        }  catch (IOException | RuntimeException | Error e) {
            MessagesServlet.addExchange(getQueryUrl(selector), selector.getRequest(), toStackTrace(e));
            throw e;
        }
    }

    private String toStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
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
        String jsonResponse = requestMetrics(webClient, selector);
        if (isNullOrEmptyString(jsonResponse)) return null;

        return LiveConfiguration.scrapeMetrics(selector, jsonResponse);
    }

    private String requestMetrics(WebClient webClient, MBeanSelector selector) throws IOException {
        String url = getQueryUrl(selector);
        String jsonResponse = webClient.withUrl(url).doPostRequest(selector.getRequest());
        MessagesServlet.addExchange(url, selector.getRequest(), jsonResponse);
        return jsonResponse;
    }

}
