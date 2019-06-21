package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import com.google.common.base.Strings;
import com.meterware.pseudoserver.HttpUserAgentTest;
import com.meterware.pseudoserver.PseudoServlet;
import com.meterware.pseudoserver.WebResource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import static io.prometheus.wls.rest.ServletConstants.AUTHENTICATION_HEADER;
import static io.prometheus.wls.rest.ServletConstants.COOKIE_HEADER;
import static javax.servlet.http.HttpServletResponse.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class WebClientImplTest extends HttpUserAgentTest {
    private static final char QUOTE = '"';

    /** A URL with a host guaranteed not to exist. */
    private static final String UNDEFINED_HOST_URL = "http://mxyptlk/";

    private WebClientFactory factory = new WebClientFactoryImpl();

    private static String sentInfo;
    private static Map<String,String> sentHeaders = new HashMap<>();
    private static final String COOKIE = "JSESSIONID=12345";
    private static final String SET_COOKIE_VALUE = COOKIE + "; path=localhost";

    @Before
    public void setUp() {
        sentInfo = null;
        sentHeaders.clear();
    }

    @Test(expected = WebClientException.class)
    public void whenUnableToReachHost_throwException() throws Exception {
        factory.createClient().withUrl(UNDEFINED_HOST_URL).doGetRequest();
    }

    @Test(expected = WebClientException.class)
    public void whenUnableToReachServer_throwException() throws Exception {
        factory.createClient().withUrl(UNDEFINED_HOST_URL).doGetRequest();
    }

    @Test
    public void whenUnprotected_sendGetToServer() throws Exception {
        final String RESPONSE = "sent this back";

        defineResource("unprotected", new PseudoServlet() {
            public WebResource getGetResponse() {
                return new WebResource(RESPONSE, "text/plain");
            }
        });

        final String response = factory.createClient().withUrl(getHostPath() + "/unprotected").doGetRequest();

        assertThat(response, equalTo(RESPONSE));
    }

    @Test
    public void whenUnprotected_sendPostToServer() throws Exception {
        final String QUERY = "sent this";

        defineResource("unprotected", new PseudoServlet() {
            public WebResource getPostResponse() {
                sentInfo = new String(getBody());
                return new WebResource("", "text/plain");
            }
        });

        factory.createClient().withUrl(getHostPath() + "/unprotected").doPostRequest(QUERY);

        assertThat(sentInfo, equalTo(QUERY));
    }

    @Test
    public void whenUnprotected_sendPutToServer() throws Exception {
        final String QUERY = "sent this";

        defineResource("unprotected_put", new PseudoServlet() {
            public WebResource getPutResponse() {
                sentInfo = new String(getBody());
                return new WebResource("", "text/plain");
            }
        });

        factory.createClient().withUrl(getHostPath() + "/unprotected_put").doPutRequest(QUERY);

        assertThat(sentInfo, equalTo(QUERY));
    }

    @Test
    public void sendAddedHeaderOnPost() throws Exception {
        defineResource("checkHeader", new PseudoServlet() {
            public WebResource getPostResponse() {
                String header = getHeader("Added-header");
                if (header == null) {
                    return new WebResource("bad request", "text/plain", SC_BAD_REQUEST);
                } else {
                    return new WebResource("", "text/plain");
                }
            }
        });

        WebClient client = factory.createClient().withUrl(getHostPath() + "/checkHeader");
        client.addHeader("Added-header", "header_value");
        client.doPostRequest("abced");
    }

    @Test
    public void whenAuthorizationHeaderDefinedOnGet_sendIt() throws Exception {
        defineResource("headers", new PseudoServlet() {
            public WebResource getGetResponse() {
                sentHeaders.put(AUTHENTICATION_HEADER, getHeader(AUTHENTICATION_HEADER));
                return new WebResource("", "text/plain");
            }
        });

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/headers");
        webClient.setAuthentication("auth-value");
        webClient.doGetRequest();

        assertThat(sentHeaders, hasEntry(AUTHENTICATION_HEADER, "auth-value"));
    }

    @Test
    public void whenAuthorizationHeaderDefinedOnPost_sendIt() throws Exception {
        defineResource("headers", new PseudoServlet() {
            public WebResource getPostResponse() {
                sentHeaders.put(AUTHENTICATION_HEADER, getHeader(AUTHENTICATION_HEADER));
                return new WebResource("", "text/plain");
            }
        });

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/headers");
        webClient.setAuthentication("auth-value");
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasEntry(AUTHENTICATION_HEADER, "auth-value"));
    }

    @Test
    public void whenSetCookieHeaderReceived_hasValue() throws Exception {
        final String SET_COOKIE_VALUE = ExporterSession.SESSION_COOKIE_PREFIX + "12345; Domain=localhost";
        defineResource("cookies", new PseudoServlet() {
            public WebResource getPostResponse() {
                WebResource webResource = new WebResource("", "text/plain");
                webResource.addHeader("Set-Cookie: " + SET_COOKIE_VALUE);
                return webResource;
            }
        });

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/cookies");
        webClient.doPostRequest("abced");

        assertThat(webClient.getSetCookieHeader(), equalTo(SET_COOKIE_VALUE));
    }

    @Test
    public void returnValueFromServer() throws Exception {
        final String RESPONSE = "returned this";

        defineResource("query", new PseudoServlet() {
            public WebResource getPostResponse() {
                return new WebResource(RESPONSE, "text/plain");
            }
        });

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/query");
        assertThat(webClient.doPostRequest("abced"), equalTo(RESPONSE));
    }

    @Test(expected = RestQueryException.class)
    public void when400StatusReceived_throwsRestQueryException() throws Exception {
        defineResource("badRestQuery", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("bad query", "text/plain", SC_BAD_REQUEST);
            }
        });

        factory.createClient().withUrl(getHostPath() + "/badRestQuery").doPostRequest("abced");
    }

    @Test(expected = AuthenticationChallengeException.class)
    public void when401ReceivedFromServer_throwsException() throws Exception {
        defineResource("protected", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                WebResource resource = new WebResource("unauthorized", "text/plain", SC_UNAUTHORIZED);
                resource.addHeader("WWW-Authenticate: Basic realm=\"REST Realm\"");
                return resource;
            }
        });

        factory.createClient().withUrl(getHostPath() + "/protected").doPostRequest("abced");
    }

    @Test
    public void when401ReceivedFromServer_exceptionIncludesRealm() throws Exception {
        defineResource("protected", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                WebResource resource = new WebResource("unauthorized", "text/plain", SC_UNAUTHORIZED);
                resource.addHeader("WWW-Authenticate: Basic realm=\"REST Realm\"");
                return resource;
            }
        });

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/protected");

        try {
            webClient.doPostRequest("abcd");
        } catch (AuthenticationChallengeException e) {
            assertThat(extractRealm(e.getChallenge()), equalTo("REST Realm"));
        }
    }

    // the value should be of the form <Basic realm="<realm-name>" and we want to extract the realm name
    private String extractRealm(String authenticationHeaderValue) {
        int start = authenticationHeaderValue.indexOf(QUOTE);
        int end = authenticationHeaderValue.indexOf(QUOTE, start+1);
        return start > 0 ? authenticationHeaderValue.substring(start+1, end) : "none";
    }

    @Test(expected = ForbiddenException.class)
    public void when403ReceivedFromServer_throwsException() throws Exception {
        defineResource("forbidden", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("not allowed", "text/plain", SC_FORBIDDEN);
            }
        });

        factory.createClient().withUrl(getHostPath() + "/forbidden").doPostRequest("abced");
    }

    @Test(expected = RestPortConnectionException.class)
    public void whenUnableToConnection_throwsException() throws IOException {
        testSupport.tearDownServer();

        for (int attempt = 0; attempt < 20; attempt++)
            accessUndefinedPort();
    }

    // try to read the just-closed port. If it is not yet closed, we get a socket exception, which we swallow to retry
    private void accessUndefinedPort() throws IOException {
        try {
            factory.createClient().withUrl(getHostPath() + "/noConnection").doPostRequest("abced");
        } catch (SocketException ignored) {
        }
    }

    @Test
    public void afterSetCookie_haveSessionCookie() throws Exception {
        final String COOKIE = ExporterSession.SESSION_COOKIE_PREFIX + "12345";
        final String SET_COOKIE_VALUE = COOKIE + "; path=localhost";
        defineResource("session", new PseudoServlet() {
            public WebResource getPostResponse() {
                WebResource webResource = new WebResource("", "text/plain");
                webResource.addHeader("Set-Cookie: " + SET_COOKIE_VALUE);
                return webResource;
            }
        });

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/session");
        webClient.doPostRequest("abced");

        assertThat(webClient.getSessionCookie(), equalTo(COOKIE));
    }

    @Test
    public void afterSetCookieReceived_sendCookieOnNextRequest() throws Exception {
        defineSessionResource();

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/session");
        webClient.doPostRequest("abced");
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasEntry(COOKIE_HEADER, COOKIE));
    }

    private void defineSessionResource() {
        defineResource("session", new PseudoServlet() {
            public WebResource getPostResponse() {
                WebResource webResource = new WebResource("", "text/plain");
                if (Strings.isNullOrEmpty(getHeader(COOKIE_HEADER)))
                    webResource.addHeader("Set-Cookie: " + SET_COOKIE_VALUE);
                else
                    recordHeaders();
                return webResource;
            }

            private void recordHeaders() {
                recordHeader(AUTHENTICATION_HEADER, getHeader(AUTHENTICATION_HEADER));
                recordHeader(COOKIE_HEADER, getHeader(COOKIE_HEADER));
            }
        });
    }

    private void recordHeader(String key, String value) {
        if (value != null) sentHeaders.put(key, value);
    }

    @Test
    public void afterSetCookieReceived_clientRetainsSetCookieOnNextRequest() throws Exception {
        defineSessionResource();

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/session");
        webClient.doPostRequest("abced");
        webClient.doPostRequest("abced");

        assertThat(webClient.getSetCookieHeader(), equalTo(SET_COOKIE_VALUE));
    }

    @Test
    public void afterSetCookieReceived_alsoSendAuthenticationOnNextRequest() throws Exception {
        defineSessionResource();

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/session");
        webClient.setAuthentication("authentication");
        webClient.doPostRequest("abced");
        sentHeaders.clear();
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasEntry(AUTHENTICATION_HEADER, "authentication"));
    }

    @Test
    public void whenStartWithSessionCookie_sendOnMultipleRequests() throws Exception {
        defineSessionResource();

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/session");
        webClient.setAuthentication("authentication");
        webClient.setSessionCookie(COOKIE);
        webClient.doPostRequest("abced");
        sentHeaders.clear();
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasEntry(COOKIE_HEADER, COOKIE));
    }

    @Test
    public void afterSessionCookieSet_sendCookieOnNextRequest() throws Exception {
        final String COOKIE = "JSESSIONID=12345";
        defineResource("session", new PseudoServlet() {
            public WebResource getPostResponse() {
                WebResource webResource = new WebResource("", "text/plain");
                sentHeaders.put(COOKIE_HEADER, getHeader(COOKIE_HEADER));
                return webResource;
            }
        });

        WebClient webClient = factory.createClient().withUrl(getHostPath() + "/session");
        webClient.setSessionCookie(COOKIE);
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasEntry(COOKIE_HEADER, COOKIE));
    }
}
