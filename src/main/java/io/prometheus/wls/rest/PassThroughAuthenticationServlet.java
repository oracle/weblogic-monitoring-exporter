package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

abstract public class PassThroughAuthenticationServlet extends HttpServlet {
    private WebClientFactory webClientFactory;

    PassThroughAuthenticationServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    private WebClient createWebClient(HttpServletRequest req) {
        LiveConfiguration.setServer(req.getServerName(), req.getServerPort());
        final WebClient webClient = webClientFactory.createClient(LiveConfiguration.getQueryUrl());
        webClient.addHeader("X-Requested-By", "rest-exporter");

        forwardRequestHeaders(req, webClient);
        return webClient;
    }

    private void forwardRequestHeaders(HttpServletRequest req, WebClient webClient) {
        webClient.establishSession(req.getHeader(ServletConstants.AUTHENTICATION_HEADER), getSessionCookie(req));
     }

    private String getSessionCookie(HttpServletRequest req) {
        for (Enumeration<String> each = req.getHeaders(ServletConstants.COOKIE_HEADER); each.hasMoreElements();) {
            String sessionCookie = ExporterSession.getSessionCookie(each.nextElement());
            if (sessionCookie != null) return sessionCookie;
        }
        return null;
    }

    void doWithAuthentication(HttpServletRequest req, HttpServletResponse resp, AuthenticatedService authenticatedService) throws IOException, ServletException {
        try {
            WebClient webClient = createWebClient(req);
            authenticatedService.execute(webClient, req, resp);
            webClient.forwardResponseHeaders(resp);
        } catch (ForbiddenException e) {
            resp.sendError(SC_FORBIDDEN, "Not authorized");
        } catch (BasicAuthenticationChallengeException e) {
            resp.setHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", e.getRealm()));
            resp.sendError(SC_UNAUTHORIZED, "Authentication required");
        } finally {
            final HttpSession session = req.getSession(false);
            if (session != null) session.invalidate();
        }
    }

    interface AuthenticatedService {
        void execute(WebClient webClient, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException;
    }

}
