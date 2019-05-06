// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package io.prometheus.wls.rest;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(value = "/" + ServletConstants.LOG_PAGE)
public class LogServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String errors = LiveConfiguration.getErrors();
        if (errors == null || errors.trim().length() == 0)
            resp.getOutputStream().println("No errors reported.");
        else
            resp.getOutputStream().println("<blockquote>" + errors + "</blockquote>");

        resp.getOutputStream().close();
    }
}
