// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.PrintStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.oracle.wls.exporter.ServletConstants.APPEND_ACTION;
import static com.oracle.wls.exporter.ServletConstants.CONFIGURATION_PAGE;
import static com.oracle.wls.exporter.ServletConstants.EFFECT_OPTION;
import static com.oracle.wls.exporter.ServletConstants.REPLACE_ACTION;

/**
 * This servlet represents the 'landing page' for the exporter.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + ServletConstants.MAIN_PAGE)
public class MainServlet extends HttpServlet {

    @Override
    public void init(ServletConfig servletConfig) {
        LiveConfiguration.init(servletConfig);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LiveConfiguration.updateConfiguration();
        LiveConfiguration.setServer(req);
        resp.getOutputStream().println(ServletConstants.PAGE_HEADER);
        displayMetricsLink(req, resp.getOutputStream());
        displayForm(req, resp.getOutputStream());
        displayConfiguration(resp.getOutputStream());

        resp.getOutputStream().close();
    }

    private void displayMetricsLink(HttpServletRequest req, ServletOutputStream outputStream) throws IOException {
        outputStream.println("<h2>This is the WebLogic Monitoring Exporter.</h2>");
        outputStream.println("<p>The metrics are found at <a href=\"" + getMetricsLink(req) + "\">");
        outputStream.println(ServletConstants.METRICS_PAGE + "</a> relative to this location.");
        outputStream.println("</p>");
    }

    private String getMetricsLink(HttpServletRequest req) {
        return getEffectivePath(req, ServletConstants.METRICS_PAGE);
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

    private void displayConfiguration(ServletOutputStream outputStream) {
        try (PrintStream ps = new PrintStream(outputStream)) {
            ps.println("<p>Current Configuration</p>");
            ps.println("<p><code><pre>");
            ps.print(LiveConfiguration.asString());
            ps.println("</pre></code></p>");
        }
    }
}
