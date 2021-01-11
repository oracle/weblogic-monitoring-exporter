// Copyright (c) 2019, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.WlsRestExchanges;

import static com.oracle.wls.exporter.WebAppConstants.MESSAGES_PAGE;

/**
 * A collector of REST requests and replies, that can be viewed to diagnose problems.
 */
@WebServlet("/" + MESSAGES_PAGE)
public class MessagesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try(ServletOutputStream out = resp.getOutputStream()) {
            for (String message : WlsRestExchanges.getExchanges())
                out.println(message);
        }
    }

}
