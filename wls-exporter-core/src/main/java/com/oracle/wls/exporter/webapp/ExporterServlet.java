// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.ExporterCall;
import com.oracle.wls.exporter.ServletInvocationContext;
import com.oracle.wls.exporter.WebAppConstants;
import com.oracle.wls.exporter.WebClientFactory;
import com.oracle.wls.exporter.WebClientFactoryImpl;

/**
 * The servlet which produces the exported metrics.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + WebAppConstants.METRICS_PAGE)
public class ExporterServlet extends HttpServlet {

    private final WebClientFactory webClientFactory;

    @SuppressWarnings("unused")  // production constructor
    public ExporterServlet() {
        this(new WebClientFactoryImpl());
    }

    public ExporterServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    public void init(ServletConfig servletConfig) {
        ServletUtils.initializeConfiguration(servletConfig);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletUtils.setServer(req);
        ExporterCall call = new ExporterCall(webClientFactory, new ServletInvocationContext(req, resp));
        call.doWithAuthentication();
    }

}
