// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.ConfigurationDisplay;
import com.oracle.wls.exporter.LiveConfiguration;
import com.oracle.wls.exporter.WebAppConstants;

import static com.oracle.wls.exporter.WebAppConstants.APPEND_ACTION;
import static com.oracle.wls.exporter.WebAppConstants.CONFIGURATION_PAGE;
import static com.oracle.wls.exporter.WebAppConstants.EFFECT_OPTION;
import static com.oracle.wls.exporter.WebAppConstants.REPLACE_ACTION;

/**
 * This servlet represents the 'landing page' for the exporter.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + WebAppConstants.MAIN_PAGE)
public class MainServlet extends HttpServlet {

    @Override
    public void init(ServletConfig servletConfig) {
        ServletUtils.initializeConfiguration(servletConfig);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LiveConfiguration.updateConfiguration();
        ServletUtils.setServer(req);
        resp.getOutputStream().println(WebAppConstants.PAGE_HEADER);
        displayMetricsLink(req, resp.getOutputStream());
        displayForm(req, resp.getOutputStream());
        ConfigurationDisplay.displayConfiguration(resp.getOutputStream());

        resp.getOutputStream().close();
    }

    private void displayMetricsLink(HttpServletRequest req, ServletOutputStream outputStream) throws IOException {
        outputStream.println("<h2>This is the WebLogic Monitoring Exporter.</h2>");
        outputStream.println("<p>The metrics are found at <a href=\"" + getMetricsLink(req) + "\">");
        outputStream.println(WebAppConstants.METRICS_PAGE + "</a> relative to this location.");
        outputStream.println("</p>");
    }

    private String getMetricsLink(HttpServletRequest req) {
        return getEffectivePath(req, WebAppConstants.METRICS_PAGE);
    }

    private String getEffectivePath(HttpServletRequest req, String relativePath) {
        if (req.getServletPath().startsWith("/"))
            return relativePath;
        else
            return req.getContextPath() + "/" + relativePath;
    }

    private void displayForm(HttpServletRequest req, ServletOutputStream outputStream) throws IOException {
        outputStream.println("<h2>Configuration</h2>");
        outputStream.println("<p>To change the configuration:</p>");
        outputStream.println("<form action=\"" + getConfigurationLink(req) + "\" method=\"post\" enctype=\"multipart/form-data\">");
        outputStream.println("    <input type=\"radio\" name=\"" + EFFECT_OPTION + "\" value=\"" + APPEND_ACTION + "\">Append");
        outputStream.println("    <input type=\"radio\" name=\"" + EFFECT_OPTION + "\" value=\"" + REPLACE_ACTION + "\" checked=\"checked\">Replace");
        outputStream.println("    <br><input type=\"file\" name=\"configuration\">");
        outputStream.println("    <br><input type=\"submit\">");
        outputStream.println("</form>");
    }

    private String getConfigurationLink(HttpServletRequest req) {
        return getEffectivePath(req, CONFIGURATION_PAGE);
    }

}
