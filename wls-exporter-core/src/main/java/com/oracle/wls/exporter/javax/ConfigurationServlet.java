// Copyright (c) 2017, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.javax;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.CallFactory;
import com.oracle.wls.exporter.CallFactoryServletImpl;
import com.oracle.wls.exporter.WebAppConstants;
import com.oracle.wls.exporter.WebClientFactory;
import com.oracle.wls.exporter.WebClientFactoryImpl;

/**
 * A servlet which handles updates to the exporter configuration.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + WebAppConstants.CONFIGURATION_PAGE)
public class ConfigurationServlet extends HttpServlet {

    private final CallFactory callFactory;

    @SuppressWarnings("unused")  // production constructor
    public ConfigurationServlet() {
        this(new WebClientFactoryImpl());
    }

    ConfigurationServlet(WebClientFactory webClientFactory) {
        this.callFactory = new CallFactoryServletImpl(webClientFactory);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            ServletUtils.setServer(request);
            callFactory.invokeConfigurationFormCall(new ServletInvocationContext(request, response));
        } catch (RuntimeException e) {
            throw new ServletException(e);
        }
    }

}
