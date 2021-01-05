// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet which handles updates to the exporter configuration.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + ServletConstants.CONFIGURATION_PAGE)
public class ConfigurationServlet extends HttpServlet {

    private final WebClientFactory webClientFactory;

    @SuppressWarnings("unused")  // production constructor
    public ConfigurationServlet() {
        this(new WebClientFactoryImpl());
    }

    ConfigurationServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            LiveConfiguration.setServer(request);
            ConfigurationCall call = new ConfigurationCall(webClientFactory, new ServletInvocationContext(request, response));
            call.doWithAuthentication();
        } catch (RuntimeException e) {
            throw new ServletException(e);
        }
    }

}
