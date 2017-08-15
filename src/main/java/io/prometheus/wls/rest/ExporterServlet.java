package io.prometheus.wls.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.prometheus.wls.rest.domain.ExporterConfig;
import io.prometheus.wls.rest.domain.MBeanSelector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import static io.prometheus.wls.rest.StatusCodes.AUTHENTICATION_REQUIRED;
import static io.prometheus.wls.rest.StatusCodes.NOT_AUTHORIZED;
import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

@WebServlet(value = "/metrics")
public class ExporterServlet extends HttpServlet {

    static final String CONFIGURATION_FILE = "configuration";
    static final String EMPTY_QUERY = "{fields:[],links:[]}";

    private static final String URL_PATTERN = "http://%s:%d/management/weblogic/latest/serverRuntime/search";
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
        InputStream configurationFile = getConfigurationFile(servletConfig);
        if (configurationFile == null) return;

        LiveConfiguration.setConfig(ExporterConfig.loadConfig(configurationFile));
        webClient.setCredentials(LiveConfiguration.getConfig().getUserName(), LiveConfiguration.getConfig().getPassword());
    }

    private InputStream getConfigurationFile(ServletConfig config) {
        String fileName = config.getInitParameter(CONFIGURATION_FILE);
        if (fileName == null)
            return null;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        webClient.defineQueryUrl(createQueryUrl(LiveConfiguration.getConfig()));
        webClient.setAuthenticationCredentials(request.getHeader("Authorization"));
        try {
            try (PrintStream ps = new PrintStream(response.getOutputStream())) {
                if (initError != null)
                    ps.println(initError);
                else if (!LiveConfiguration.hasQueries())
                    ps.println("# No configuration defined.");
                else
                    printMetrics(ps);
            }
        } catch (NotAuthorizedException e) {
            response.sendError(NOT_AUTHORIZED, "Not authorized");
        } catch (BasicAuthenticationChallengeException e) {
            response.setHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", e.getRealm()));
            response.sendError(AUTHENTICATION_REQUIRED, "Authentication required");
        }
    }

    private void printMetrics(PrintStream ps) throws IOException {
        for (MBeanSelector selector : LiveConfiguration.getConfig().getQueries())
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
