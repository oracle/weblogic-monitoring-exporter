package io.prometheus.wls.rest;

import io.prometheus.wls.rest.domain.ExporterConfig;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ExporterServlet extends HttpServlet {

    private WebClient webClient;

    @SuppressWarnings("WeakerAccess")
    public ExporterServlet() {
        this(new WebClientImpl());
    }

    ExporterServlet(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        try {
            ExporterConfig config = ExporterConfig.loadConfig(Files.openFile(servletConfig.getInitParameter("configuration")));
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
        super.doGet(req, resp);
    }
}
