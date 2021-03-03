// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.LiveConfiguration;
import com.oracle.wls.exporter.WebAppConstants;

@WebServlet(value = "/" + WebAppConstants.LOG_PAGE)
public class LogServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String errors = LiveConfiguration.getErrors();
        if (errors == null || errors.trim().length() == 0)
            resp.getOutputStream().println("No errors reported.");
        else
            resp.getOutputStream().println("<blockquote>" + errors + "</blockquote>");

        resp.getOutputStream().close();
    }
}
