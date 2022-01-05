// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.oracle.wls.exporter.domain.MBeanSelector;
import com.oracle.wls.exporter.domain.QueryType;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * A base context which performs authentication by forwarding all pertinent headers between the client
 * and the WLS RESTful Management services, thus using that service's security.
 */
public abstract class AuthenticatedCall {
    private final WebClientFactory webClientFactory;
    private final InvocationContext context;
    private final UrlBuilder urlBuilder;


    public AuthenticatedCall(WebClientFactory webClientFactory, InvocationContext context) {
        this.webClientFactory = webClientFactory;
        this.context = context;
        this.urlBuilder = context.createUrlBuilder();
    }

    public String getAuthenticationUrl() {
        return urlBuilder.createUrl(QueryType.RUNTIME_URL_PATTERN);
    }

    String getQueryUrl(MBeanSelector selector) {
        return urlBuilder.createUrl(selector.getQueryType().getUrlPattern());
    }

    void reportFailure(RestPortConnectionException e) {
        urlBuilder.reportFailure(e);
    }

    public WebClient createWebClient() {
        final WebClient webClient = webClientFactory.createClient();
        webClient.addHeader("X-Requested-By", "rest-exporter");

        setAuthentication(webClient);
        return webClient;
    }

    protected void setAuthentication(WebClient webClient) {
        webClient.setAuthentication(context.getAuthenticationHeader());
     }

    /**
     * Performs a servlet action, wrapping it with authentication handling.
     *
     * This involves creating an object which can make http calls to the RESTful services, configuring it to
     * include the authentication header, if any, received from client. Any authentication errors from
     * the services will be returned to the client.
     *
     * @throws IOException if an error occurs in the web client
     */
    public void doWithAuthentication() throws IOException {
        try {
            WebClient webClient = createWebClient();
            performRequest(webClient);
            urlBuilder.reportSuccess();
        } catch (ForbiddenException e) {
            context.sendError(HTTP_FORBIDDEN, "Not authorized");
        } catch (AuthenticationChallengeException e) {
            context.setResponseHeader("WWW-Authenticate", e.getChallenge());
            context.sendError(HTTP_UNAUTHORIZED, "Authentication required");
        } catch (ServerErrorException e) {
            final int status = e.getStatus();
            context.sendError(status, e.getMessage());
        } catch (RestPortConnectionException e) {
            context.setStatus(HTTP_INTERNAL_ERROR);
            reportUnableToContactRestApiPort(getFailedHosts());
        } finally {
            context.close();
        }
    }

    private String getFailedHosts() {
        final List<String> hosts = urlBuilder.getFailedHosts();
        if (hosts.size() < 3) {
            return String.join(" or ", hosts);
        } else {
            return String.join(", ", hosts.subList(0, hosts.size()-1)) + " or " + hosts.get(hosts.size() - 1);
        }
    }

    /**
     * Returns a name for the instance for which the exporter is reporting metrics.
     */
    public String getInstanceName() {
        return context.getInstanceName();
    }

    /**
     * Returns the MIME type of the data in the request stream.
     */
    public String getRequestContentType() {
        return context.getContentType();
    }


    private void performRequest(WebClient webClient) throws IOException {
        do {
            invoke(webClient, context);
        } while (webClient.isRetryNeeded());
    }

    private void reportUnableToContactRestApiPort(String uri) throws IOException {
        try (PrintStream out = context.getResponseStream()) {
            out.println("# Unable to contact the REST API at " + uri + ". May be using the wrong host name or port.");
            out.println("#");
            out.println("# This can happen when the exporter is accessed via a load balancer");
            out.println("# configured on a different port than the server's REST API,");
            out.println("# or when the remote server DNS name is not resolvable from the server itself.");
            out.println("#");
            out.println("# You can correct this by giving the exporter WAR an initial configuration with the");
            out.println("# restPort field set to the managed server's plain text port and/or");
            out.println("# restHostName field set to the managed server's locally resolvable name.");
        }
    }

    /**
     * Performs an action that requires authentication from WebLogic's REST API using the current invocation context.
     * @param webClient the client to send the request
     * @param context an invocation context
     * @throws IOException if an error occurs
     */
    protected abstract void invoke(WebClient webClient, InvocationContext context) throws IOException;

}
