// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.oracle.wls.exporter.domain.MBeanSelector;
import com.oracle.wls.exporter.domain.QueryType;

import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_CHALLENGE_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.COOKIE_HEADER;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * A base context which performs authentication by forwarding all pertinent headers between the client
 * and the WLS RESTful Management services, thus using that service's security.
 */
public abstract class AuthenticatedCall {

    /**
     * The length of time in seconds after which a cookie is deemed to have expired.
     */
    static final long COOKIE_LIFETIME_SECONDS = 1000L;

    /**
     * A map of authentication credentials to lists of cookies. The cookies will be sent on any request with
     * the specified credentials.
     */
    private static final Map<String,List<Cookie>> COOKIES = new HashMap<>();

    private final WebClientFactory webClientFactory;
    private final InvocationContext context;
    private final UrlBuilder urlBuilder;

    // For unit testing only
    static void clearCookies() {
        COOKIES.clear();
    }

    protected AuthenticatedCall(WebClientFactory webClientFactory, InvocationContext context) {
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

        webClient.setAuthentication(context.getAuthenticationHeader());
        getCookies(context.getAuthenticationHeader()).forEach(c -> webClient.addHeader(COOKIE_HEADER, c));
        webClient.onSetCookieReceivedDo(this::handleNewCookie);
        webClient.onSetCookieReceivedDo(c -> webClient.addHeader(COOKIE_HEADER, c));
        return webClient;
    }

    public List<String> getCookies(String credentials) {
        final OffsetDateTime now = SystemClock.now();
        final List<Cookie> cookieList = getCookieList(credentials);
        cookieList.removeIf(c -> c.isExpiredAt(now));
        
        return cookieList.stream().map(Cookie::getValue).collect(Collectors.toList());
    }

    private List<Cookie> getCookieList(String credentials) {
        return Optional.ofNullable(COOKIES.get(credentials)).orElse(Collections.emptyList());
    }

    void handleNewCookie(String cookieHeader) {
        if (context.getAuthenticationHeader() == null) return;

        final Cookie cookie = new Cookie(cookieHeader);
        COOKIES
              .computeIfAbsent(context.getAuthenticationHeader(), h -> new ArrayList<>())
              .add(cookie);
    }

    private static class Cookie {
        private final String value;
        private final OffsetDateTime expirationTime = SystemClock.now().plusSeconds(COOKIE_LIFETIME_SECONDS);

        Cookie(String cookieHeader) {
            this.value = trimParameters(cookieHeader);
        }

        String getValue() {
            return value;
        }

        boolean isExpiredAt(OffsetDateTime now) {
            return now.isAfter(expirationTime);
        }

        private String trimParameters(String cookieHeader) {
          if (!cookieHeader.contains(";"))
            return cookieHeader;
          else
            return cookieHeader.substring(0, cookieHeader.indexOf(';'));
        }
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
            context.setResponseHeader(AUTHENTICATION_CHALLENGE_HEADER, e.getChallenge());
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
