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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

public class ExporterServlet extends HttpServlet {

    static final String CONFIGURATION_FILE = "configuration";
    private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
    private WebClient webClient;
    private ExporterConfig config;
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
        InputStream configurationFile = getConfigurationFile(servletConfig);
        if (configurationFile == null) return;

        config = ExporterConfig.loadConfig(configurationFile);
        webClient.initialize(createQueryUrl(config), config.getUserName(), config.getPassword());
    }

    private InputStream getConfigurationFile(ServletConfig config) {
        String fileName = config.getInitParameter(CONFIGURATION_FILE);
        if (fileName == null)
            return configurationNotFound("No value specified for init parameter '" + CONFIGURATION_FILE + "'");
        else if (!fileName.startsWith("/"))
            return configurationNotFound("init parameter '" + CONFIGURATION_FILE + "' must start with '/'");

        InputStream configurationFile = config.getServletContext().getResourceAsStream(fileName);
        if (configurationFile == null)
            return configurationNotFound("Configuration file not found at " + fileName);

        return configurationFile;
     }

    private InputStream configurationNotFound(String message) {
        initError = message;
        return null;
    }

    private String createQueryUrl(ExporterConfig config) {
        return String.format(URL_PATTERN, config.getHost(), config.getPort() );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (PrintStream ps = new PrintStream(resp.getOutputStream())) {
            if (initError != null)
                ps.println(initError);
            else
                printMetrics(ps);
        }
    }

    private void printMetrics(PrintStream ps) throws IOException {
        for (MBeanSelector selector : config.getQueries())
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

        return selector.scrapeMetrics(toJsonObject(jsonResponse));
    }

    private JsonObject toJsonObject(String response) {
        return new JsonParser().parse(response).getAsJsonObject();
    }
}
