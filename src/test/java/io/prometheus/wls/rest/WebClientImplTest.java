package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.meterware.httpunit.Base64;
import com.meterware.pseudoserver.HttpUserAgentTest;
import com.meterware.pseudoserver.PseudoServlet;
import com.meterware.pseudoserver.WebResource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.prometheus.wls.rest.ServletConstants.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class WebClientImplTest extends HttpUserAgentTest {

    private WebClientFactory factory = new WebClientFactoryImpl();

    private static String sentInfo;
    private static Map<String,String> sentHeaders = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        sentInfo = null;
        sentHeaders.clear();
    }

    @Test
    public void whenUnprotected_sendQueryToServer() throws Exception {
        final String QUERY = "sent this";

        defineResource("unprotected", new PseudoServlet() {
            public WebResource getPostResponse() {
                sentInfo = new String(getBody());
                return new WebResource("", "text/plain");
            }
        });

        factory.createClient(getHostPath() + "/unprotected").doQuery(QUERY);

        assertThat(sentInfo, equalTo(QUERY));
    }

    @Test
    public void sendXRequestedByHeader() throws Exception {
        defineResource("checkXRequested", new PseudoServlet() {
            public WebResource getPostResponse() {
                String header = getHeader("X-Requested-By");
                if (header == null) {
                    return new WebResource("bad request", "text/plain", BAD_REQUEST);
                } else {
                    return new WebResource("", "text/plain");
                }
            }
        });

        factory.createClient(getHostPath() + "/checkXRequested").doQuery("abced");
    }

    @Test
    public void sendCredentialsWhenChallenged() throws Exception {
        defineResource("challenge", new PseudoServlet() {
            public WebResource getPostResponse() {
                String header = getHeader("Authorization");
                if (header == null) {
                    WebResource webResource = new WebResource("unauthorized", "text/plain", AUTHENTICATION_REQUIRED);
                    webResource.addHeader("WWW-Authenticate: Basic realm=\"testrealm\"");
                    return webResource;
                } else {
                    sentHeaders.put("Authorization", header);
                    return new WebResource("", "text/plain");
                }
            }
        });

        factory.setCredentials("user", "password");
        factory.createClient(getHostPath() + "/challenge").doQuery("abced");

        assertThat(sentHeaders, hasEntry("Authorization", "Basic " + Base64.encode("user:password")));
    }

    @Test
    public void whenHeadersDefined_sendThem() throws Exception {
        defineResource("headers", new PseudoServlet() {
            public WebResource getPostResponse() {
                sentHeaders.put("header1", getHeader("header1"));
                sentHeaders.put("header2", getHeader("header2"));
                return new WebResource("", "text/plain");
            }
        });

        WebClient webClient = factory.createClient(getHostPath() + "/headers");
        webClient.putHeader("header1", "value1");
        webClient.putHeader("header2", "value2");
        webClient.doQuery("abced");

        assertThat(sentHeaders, hasEntry("header1", "value1"));
        assertThat(sentHeaders, hasEntry("header2", "value2"));
    }

    @Test
    public void whenSetCookieHeaderReceived_hasValue() throws Exception {
        final String SET_COOKIE_VALUE = "jsession=12345; Domain=localhost";
        defineResource("cookies", new PseudoServlet() {
            public WebResource getPostResponse() {
                WebResource webResource = new WebResource("", "text/plain");
                webResource.addHeader("Set-Cookie: " + SET_COOKIE_VALUE);
                return webResource;
            }
        });

        WebClient webClient = factory.createClient(getHostPath() + "/cookies");
        webClient.doQuery("abced");

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

        WebClient webClient = factory.createClient(getHostPath() + "/query");
        assertThat(webClient.doQuery("abced"), equalTo(RESPONSE));
    }

    @Test(expected = RestQueryException.class)
    public void when400StatusReceived_throwsRestQueryException() throws Exception {
        defineResource("badRestQuery", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() throws IOException {
                return new WebResource("bad query", "text/plain", BAD_REQUEST);
            }
        });

        factory.createClient(getHostPath() + "/badRestQuery").doQuery("abced");
    }

    @Test(expected = BasicAuthenticationChallengeException.class)
    public void when401ReceivedFromServer_throwsException() throws Exception {
        defineResource("protected", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() throws IOException {
                WebResource resource = new WebResource("unauthorized", "text/plain", AUTHENTICATION_REQUIRED);
                resource.addHeader("WWW-Authenticate: Basic realm=\"REST Realm\"");
                return resource;
            }
        });

        factory.createClient(getHostPath() + "/protected").doQuery("abced");
    }

    @Test
    public void when401ReceivedFromServer_exceptionIncludesRealm() throws Exception {
        defineResource("protected", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() throws IOException {
                WebResource resource = new WebResource("unauthorized", "text/plain", AUTHENTICATION_REQUIRED);
                resource.addHeader("WWW-Authenticate: Basic realm=\"REST Realm\"");
                return resource;
            }
        });

        WebClient webClient = factory.createClient(getHostPath() + "/protected");

        try {
            webClient.doQuery("abcd");
        } catch (BasicAuthenticationChallengeException e) {
            assertThat(e.getRealm(), equalTo("REST Realm"));
        }
    }

    @Test(expected = NotAuthorizedException.class)
    public void when403ReceivedFromServer_throwsException() throws Exception {
        defineResource("forbidden", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() throws IOException {
                return new WebResource("not allowed", "text/plain", NOT_AUTHORIZED);
            }
        });

        factory.createClient(getHostPath() + "/forbidden").doQuery("abced");
    }
}
