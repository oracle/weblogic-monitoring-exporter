package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2020, Oracle Corporation and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import io.prometheus.wls.rest.domain.Protocol;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * An abstract servlet which performs authentication by forwarding all pertinent headers between the client
 * and the WLS RESTful Management services, thus using that service's security.
 */
abstract public class PassThroughAuthenticationServlet extends HttpServlet {
    private final WebClientFactory webClientFactory;
    private Protocol protocol;

    PassThroughAuthenticationServlet(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    private WebClient createWebClient(HttpServletRequest req) {
        LiveConfiguration.setServer(req.getServerName(), req.getServerPort());
        final WebClient webClient = webClientFactory.createClient();
        webClient.addHeader("X-Requested-By", "rest-exporter");

        forwardRequestHeaders(req, webClient);
        protocol = Protocol.getProtocol(req);
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

    /**
     * Performs a servlet action, wrappering it with authentication handling.
     *
     * This involves creating an object which can make http calls to the RESTful services, configuring it to
     * include the authentication header, if any, received from client. Any authentication errors from
     * the services will be returned to the client.
     *
     * @param req the servlet request
     * @param resp the servlet response
     * @param authenticatedService an which actually performs calls to the RESTful services using the supplied client
     *
     * @throws IOException if an error occurs in the web client
     * @throws ServletException if some other fatal error occurs
     */
    void doWithAuthentication(HttpServletRequest req, HttpServletResponse resp, AuthenticatedService authenticatedService) throws IOException, ServletException {
        try {
            WebClient webClient = createWebClient(req);
            authenticatedService.execute(webClient, req, resp);
            webClient.forwardResponseHeaders(resp);
        } catch (ForbiddenException e) {
            resp.sendError(SC_FORBIDDEN, "Not authorized");
        } catch (AuthenticationChallengeException e) {
            resp.setHeader("WWW-Authenticate", e.getChallenge());
            resp.sendError(SC_UNAUTHORIZED, "Authentication required");
        } catch (ServerErrorException e) {
            resp.sendError(e.getStatus());
        } catch (RestPortConnectionException e) {
            resp.setStatus(SC_INTERNAL_SERVER_ERROR);
            reportUnableToContactRestApi(resp, e.getUri());
        } finally {
            final HttpSession session = req.getSession(false);
            if (session != null) session.invalidate();
        }
    }

    protected Protocol getProtocol() {
        return protocol;
    }

    private void reportUnableToContactRestApi(HttpServletResponse resp, String uri) throws IOException {
        try (ServletOutputStream out = resp.getOutputStream()) {
            out.println("# Unable to contact the REST API at " + uri + ". May be using the wrong port.");
            out.println("#");
            out.println("# This most commonly occurs when the exporter is accessed via a load balancer");
            out.println("# configured on a different port than the managed server.");
            out.println("#");
            out.println("# You can correct this by giving the exporter WAR an initial configuration with the");
            out.println("# restPort field set to the managed server's plain text port.");
        }
    }

    /**
     * An interface describing the service wrapped by #doWithAuthentication
     */
    interface AuthenticatedService {
        void execute(WebClient webClient, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException;
    }

}
