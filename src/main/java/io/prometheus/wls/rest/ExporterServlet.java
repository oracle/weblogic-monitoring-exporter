package io.prometheus.wls.rest;

import io.prometheus.wls.rest.domain.MBeanSelector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import static io.prometheus.wls.rest.StatusCodes.AUTHENTICATION_REQUIRED;
import static io.prometheus.wls.rest.StatusCodes.NOT_AUTHORIZED;
import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

@WebServlet(value = "/metrics")
public class ExporterServlet extends HttpServlet {

    static final String EMPTY_QUERY = "{fields:[],links:[]}";

    private WebClient webClient;
    private String initError;

    @SuppressWarnings("unused")  // production constructor
    public ExporterServlet() {
        this(new WebClientImpl());
    }

    ExporterServlet(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        LiveConfiguration.init(servletConfig);
        webClient.setCredentials(LiveConfiguration.getUserName(), LiveConfiguration.getPassword());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LiveConfiguration.setServer(req.getServerName(), req.getServerPort());
        webClient.defineQueryUrl(LiveConfiguration.getQueryUrl());
        webClient.setAuthenticationCredentials(req.getHeader("Authorization"));
        try {
            try (PrintStream ps = new PrintStream(resp.getOutputStream())) {
                if (!LiveConfiguration.hasQueries())
                    ps.println("# No configuration defined.");
                else
                    printMetrics(ps);
            }
        } catch (NotAuthorizedException e) {
            resp.sendError(NOT_AUTHORIZED, "Not authorized");
        } catch (BasicAuthenticationChallengeException e) {
            resp.setHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", e.getRealm()));
            resp.sendError(AUTHENTICATION_REQUIRED, "Authentication required");
        }
    }

    private void printMetrics(PrintStream ps) throws IOException {
        for (MBeanSelector selector : LiveConfiguration.getQueries())
            displayMetrics(ps, selector);
    }

    private void displayMetrics(PrintStream ps, MBeanSelector selector) throws IOException {
        try {
            Map<String, Object> metrics = getMetrics(selector);
            if (metrics != null)
                sort(metrics).forEach((name, value) -> ps.println(name + " " + value));
        } catch (RestQueryException e) {
            ps.println("REST service was unable to handle this query\n" + selector.getPrintableRequest());
        }
    }

    private TreeMap<String, Object> sort(Map<String, Object> metrics) {
        return new TreeMap<>(metrics);
    }

    private Map<String, Object> getMetrics(MBeanSelector selector) throws IOException {
        String jsonResponse = webClient.doQuery(selector.getRequest());
        if (isNullOrEmptyString(jsonResponse)) return null;

        return LiveConfiguration.scrapeMetrics(selector, jsonResponse);
    }

}
