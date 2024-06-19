// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import com.oracle.wls.exporter.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintStream;

import static com.oracle.wls.exporter.WebAppConstants.QUERIES_PAGE;

@WebServlet("/" + QUERIES_PAGE)
public class QueriesServlet extends HttpServlet {

    private final WebClientFactory webClientFactory;

    public QueriesServlet() {
        this(new WebClientFactoryImpl());
    }

    public QueriesServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            ServletUtils.setServer(req);
            QueriesCall call = new QueriesCall(webClientFactory, new ServletInvocationContext(req, resp));
            call.doWithAuthentication();
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static class QueriesCall extends AuthenticatedCall {

        public QueriesCall(WebClientFactory webClientFactory, InvocationContext context) {
            super(webClientFactory, context);
        }

        @Override
        protected void invoke(WebClient webClient, InvocationContext context) throws IOException {
            try (PrintStream out = context.getResponseStream()) {
                out.println(ExporterQueries.getQueryHeader());
                for (String message : ExporterQueries.getQueryReport())
                    out.println(message);
            }
        }
    }
}
