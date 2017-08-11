package io.prometheus.wls.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.prometheus.wls.rest.domain.ExporterConfig;
import io.prometheus.wls.rest.domain.MBeanSelector;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.prometheus.wls.rest.StatusCodes.AUTHENTICATION_REQUIRED;
import static io.prometheus.wls.rest.StatusCodes.NOT_AUTHORIZED;
import static io.prometheus.wls.rest.domain.MapUtils.isNullOrEmptyString;

public class ExporterServlet extends HttpServlet {

    static final String CONFIGURATION_FILE = "configuration";
    static final String EMPTY_QUERY = "{fields:[],links:[]}";

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
        webClient.initialize(createQueryUrl(config));
        webClient.setCredentials(config.getUserName(), config.getPassword());
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        webClient.setAuthenticationCredentials(request.getHeader("Authorization"));
        try {
            try (PrintStream ps = new PrintStream(response.getOutputStream())) {
                if (initError != null)
                    ps.println(initError);
                else if (config == null || config.getQueries() == null)
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        webClient.setAuthenticationCredentials(request.getHeader("Authorization"));
        try {
            webClient.doQuery(EMPTY_QUERY);
            if (!ServletFileUpload.isMultipartContent(request)) throw new ServletException("Must be a multi-part request");

            createPostAction(request).perform();
            reportUpdatedConfiguration(response);
        } catch (NotAuthorizedException e) {
            response.sendError(NOT_AUTHORIZED, "Not authorized");
        } catch (BasicAuthenticationChallengeException e) {
            response.setHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", e.getRealm()));
            response.sendError(AUTHENTICATION_REQUIRED, "Authentication required");
        }
    }

    private void reportUpdatedConfiguration(HttpServletResponse response) throws IOException {
        try (PrintStream ps = new PrintStream(response.getOutputStream())) {
            ps.println("<html><head><title>Updated Configuration</title></head>");
            ps.println("<body<h1>Updated Configuration</h1><p><code><pre>");
            ps.printf(config.toString());
            ps.println("</pre></code></p>");
            ps.println("</body></html>");
        }
    }

    private PostAction createPostAction(HttpServletRequest request) throws IOException, ServletException {
        PostAction postAction = new PostAction();
        try {
            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
            configure(postAction, upload.parseRequest(request));
        } catch (FileUploadException e) {
            throw new ServletException("unable to parse post body", e);
        }
        return postAction;
    }

    private void configure(PostAction postAction, List<FileItem> fileItems) throws IOException {
        for (FileItem item : fileItems) {
            if (!item.isFormField()) {
                postAction.defineUploadedFile(item.getInputStream());
            } else if (item.getFieldName().equals("effect"))
                postAction.setEffect(item.getString());
        }
    }

    private class PostAction {
        private String effect = "replace";
        private ExporterConfig uploadedConfig;

        private void defineUploadedFile(InputStream inputStream) {
            uploadedConfig = ExporterConfig.loadConfig(inputStream);
        }

        void perform() throws ServletException {
            if (config == null)
                throw new ServletException("Exporter Servlet not initialized");
            if (uploadedConfig == null)
                throw new ServletException("No configuration specified");

            if (effect.equalsIgnoreCase("replace"))
                config.replace(uploadedConfig);
            else if (effect.equalsIgnoreCase("append"))
                config.append(uploadedConfig);
        }

        void setEffect(String effect) {
            this.effect = effect;
        }
    }
}
