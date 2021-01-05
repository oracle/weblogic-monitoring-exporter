// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The servlet which produces the exported metrics.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + ServletConstants.METRICS_PAGE)
public class ExporterServlet extends HttpServlet {

    private final WebClientFactory webClientFactory;

    @SuppressWarnings("unused")  // production constructor
    public ExporterServlet() {
        this(new WebClientFactoryImpl());
    }

    ExporterServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    public void init(ServletConfig servletConfig) {
        LiveConfiguration.init(servletConfig);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LiveConfiguration.setServer(req);
        ExporterCall call = new ExporterCall(webClientFactory, new ServletInvocationContext(req, resp));
        call.doWithAuthentication();
    }

}
