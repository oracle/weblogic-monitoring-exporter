package io.prometheus.wls.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.prometheus.wls.rest.domain.ExporterConfig;
import io.prometheus.wls.rest.domain.MBeanSelector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

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
        try (PrintStream ps = new PrintStream(resp.getOutputStream())) {
            for (MBeanSelector selector : config.getQueries())
                displayMetrics(ps, selector);
        }
    }

    private void displayMetrics(PrintStream ps, MBeanSelector selector) throws IOException {
        Map<String, Object> metrics = getMetrics(selector);
        if (metrics != null)
            metrics.forEach((name, value) -> ps.println(name + " " + value));
    }

    private Map<String, Object> getMetrics(MBeanSelector selector) throws IOException {
        String jsonResponse = webClient.doQuery(selector.getRequest());
        if (isNullOrEmptyString(jsonResponse)) return null;

        return selector.scrapeMetrics(toJsonObject(jsonResponse));
    }

    private JsonObject toJsonObject(String response) {
        return new JsonParser().parse(response).getAsJsonObject();
    }
}
