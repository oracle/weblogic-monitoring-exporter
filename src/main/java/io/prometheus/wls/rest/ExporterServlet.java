package io.prometheus.wls.rest;

import io.prometheus.wls.rest.domain.MBeanSelector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static io.prometheus.wls.rest.StatusCodes.AUTHENTICATION_REQUIRED;
import static io.prometheus.wls.rest.StatusCodes.NOT_AUTHORIZED;
import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

@WebServlet(value = "/metrics")
public class ExporterServlet extends HttpServlet {

    private static final String[] FORWARDED_REQUEST_HEADERS = {"Authorization", "Cookie"};

    private WebClientFactory webClientFactory;

    @SuppressWarnings("unused")  // production constructor
    public ExporterServlet() {
        this(new WebClientFactoryImpl());
    }

    ExporterServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        LiveConfiguration.init(servletConfig);
        webClientFactory.setCredentials(LiveConfiguration.getUserName(), LiveConfiguration.getPassword());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LiveConfiguration.setServer(req.getServerName(), req.getServerPort());
        WebClient webClient = webClientFactory.createClient(LiveConfiguration.getQueryUrl());
        forwardRequestHeaders(webClient, req);

        try (MetricsStream metricsStream = new MetricsStream(resp.getOutputStream())) {
            if (!LiveConfiguration.hasQueries())
                metricsStream.println("# No configuration defined.");
            else
                printMetrics(webClient, metricsStream);

            forwardResponseHeaders(webClient, resp);
        } catch (NotAuthorizedException e) {
            resp.sendError(NOT_AUTHORIZED, "Not authorized");
        } catch (BasicAuthenticationChallengeException e) {
            resp.setHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", e.getRealm()));
            resp.sendError(AUTHENTICATION_REQUIRED, "Authentication required");
        }
    }

    private void forwardRequestHeaders(WebClient webClient, HttpServletRequest req) {
        for (String headerKey : FORWARDED_REQUEST_HEADERS)
            webClient.putHeader(headerKey, req.getHeader(headerKey));
    }

    private void forwardResponseHeaders(WebClient webClient, HttpServletResponse resp) {
        if (webClient.getSetCookieHeader() != null) resp.setHeader("Set-Cookie", webClient.getSetCookieHeader());
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
            metricsStream.println("REST service was unable to handle this query\n" + selector.getPrintableRequest());
        }
    }

    private TreeMap<String, Object> sort(Map<String, Object> metrics) {
        return new TreeMap<>(metrics);
    }

    private Map<String, Object> getMetrics(WebClient webClient, MBeanSelector selector) throws IOException {
        String jsonResponse = webClient.doQuery(selector.getRequest());
        if (isNullOrEmptyString(jsonResponse)) return null;

        return LiveConfiguration.scrapeMetrics(selector, jsonResponse);
    }

}
