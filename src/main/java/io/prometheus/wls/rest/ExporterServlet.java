package io.prometheus.wls.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.prometheus.wls.rest.domain.ExporterConfig;
import io.prometheus.wls.rest.domain.MapUtils;
import io.prometheus.wls.rest.domain.MetricsScraper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class ExporterServlet extends HttpServlet {

    static final String CONFIGURATION_FILE = "configuration";
    private WebClient webClient;
    private ExporterConfig config;

    @SuppressWarnings("unused")  // production constructor

    public ExporterServlet() {
        this(new WebClientImpl());
    }

    ExporterServlet(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        try {
            config = ExporterConfig.loadConfig(Files.openFile(servletConfig.getInitParameter(CONFIGURATION_FILE)));
            webClient.initialize(createQueryUrl(config), config.getUserName(), config.getPassword());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String createQueryUrl(ExporterConfig config) {
        return String.format("http://%s:%d/management/weblogic/latest/serverRuntime", config.getHost(), config.getPort() );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String response = webClient.doQuery(config.getQueries()[0].getRequest());

        if (MapUtils.isNotNullOrEmptyString(response))
            generateMetricsFromJsonResponse(resp, response);
    }

    private void generateMetricsFromJsonResponse(HttpServletResponse resp, String response) throws IOException {
        MetricsScraper scraper = new MetricsScraper();
        scraper.scrape(config.getQueries()[0], toJsonObject(response));

        try (PrintStream ps = new PrintStream(resp.getOutputStream())) {
            scraper.getMetrics().forEach((s, o) -> ps.println(s + " " + o));
        }
    }

    private JsonObject toJsonObject(String response) {
        return new JsonParser().parse(response).getAsJsonObject();
    }
}
