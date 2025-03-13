// Copyright (c) 2019, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.javax;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.MessagesCall;
import com.oracle.wls.exporter.WebClientFactory;
import com.oracle.wls.exporter.WebClientFactoryImpl;

import static com.oracle.wls.exporter.WebAppConstants.MESSAGES_PAGE;

/**
 * A collector of REST requests and replies, that can be viewed to diagnose problems.
 */
@WebServlet("/" + MESSAGES_PAGE)
public class MessagesServlet extends HttpServlet {

    private final WebClientFactory webClientFactory;

    @SuppressWarnings("unused")
    public MessagesServlet() {
        this(new WebClientFactoryImpl());
    }

    public MessagesServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ServletUtils.setServer(req);
            MessagesCall call = new MessagesCall(webClientFactory, new ServletInvocationContext(req, resp));
            call.doWithAuthentication();
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
