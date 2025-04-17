// Copyright 2017, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.meterware.pseudoserver.PseudoServer;
import com.meterware.pseudoserver.PseudoServlet;
import com.meterware.pseudoserver.WebResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.WebAppConstants.AUTHENTICATION_HEADER;
import static com.oracle.wls.exporter.WebAppConstants.CONTENT_TYPE_HEADER;
import static com.oracle.wls.exporter.WebClient.X_REQUESTED_BY_HEADER;
import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_GATEWAY_TIMEOUT;
import static javax.servlet.http.HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Russell Gold
 */
abstract class WebClientTestBase {
    private static final char QUOTE = '"';

    /** A URL on a known hostName with a port on which no server is listening. */
    private static final String UNDEFINED_PORT_URL = "http://localhost:59236";

    private final Supplier<WebClient> factory;

    private static String sentInfo;
    private static final Map<String,String> sentHeaders = new HashMap<>();

    public WebClientTestBase(Supplier<WebClient> factory) {
        this.factory = factory;
    }
    private String hostPath;
    private PseudoServer server;


    @BeforeEach
    public void setUp() throws IOException {
        server = new PseudoServer();
        hostPath = "http://localhost:" + server.getConnectedPort();
        sentInfo = null;
        sentHeaders.clear();
    }

    @AfterEach
    public void tearDown() {
        tearDownServer();
    }

    private void tearDownServer() {
        if (server != null) server.shutDown();
    }

    @Test
    public void whenUnableToReachServer_throwException() {
        final WebClient webClient = factory.get().withUrl(UNDEFINED_PORT_URL);

        assertThrows(WebClientException.class, webClient::doGetRequest);
    }

    @Test
    public void whenUnprotected_sendGetToServer() throws Exception {
        final String RESPONSE = "sent this back";

        defineResource("unprotected", new PseudoServlet() {
            @Override
            public WebResource getGetResponse() {
                return new WebResource(RESPONSE, "text/plain");
            }
        });

        final String response = withWebClient("unprotected").doGetRequest();

        assertThat(response, equalTo(RESPONSE));
    }

    private String getHostPath() {
        return hostPath;
    }

    public void defineResource(String resourceName, PseudoServlet servlet) {
        server.setResource(resourceName, servlet);
    }

    @Test
    public void whenUnprotected_sendPostToServer() throws Exception {
        final String QUERY = "sent this";

        defineResource("unprotected", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                sentInfo = new String(getBody());
                return new WebResource("", "text/plain");
            }
        });

        withWebClient("unprotected").doPostRequest(QUERY);

        assertThat(sentInfo, equalTo(QUERY));
    }

    private WebClient withWebClient(String path) {
        return factory.get().withUrl(getHostPath() + "/" + path);
    }

    @Test
    public void whenUnprotected_sendPutToServer() throws Exception {
        final Structure QUERY = new Structure(3, "red");

        defineResource("unprotected_put", new PseudoServlet() {
            @Override
            public WebResource getPutResponse() {
                sentInfo = new String(getBody());
                return new WebResource("", "text/plain");
            }
        });

        withWebClient("unprotected_put").doPutRequest(QUERY);

        assertThat(JsonParser.parseString(sentInfo), equalTo(JsonParser.parseString(QUERY.getAsJson())));
    }

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    static class Structure {
        private final int count;
        private final String color;

        Structure(int count, String color) {
            this.count = count;
            this.color = color;
        }

        String getAsJson() {
            return new Gson().toJson(this);
        }
    }

    @Test
    public void sendAddedHeaderOnPost() {
        defineResource("checkHeader", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                String header = getHeader("Added-header");
                if (header == null) {
                    return new WebResource("bad request", "text/plain", SC_BAD_REQUEST);
                } else {
                    return new WebResource("", "text/plain");
                }
            }
        });

        WebClient client = withWebClient("checkHeader");
        client.addHeader("Added-header", "header_value");
        assertDoesNotThrow(() -> client.doPostRequest("abced"));
    }

    @Test
    public void whenPostCreated_defaultContentTypeIsJson() throws IOException {
        defineResource("headers", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                sentHeaders.put(CONTENT_TYPE_HEADER, getHeader(CONTENT_TYPE_HEADER));
                return new WebResource("", "text/plain");
            }
        });

        WebClient webClient = withWebClient("headers");
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasEntry(CONTENT_TYPE_HEADER, WebClient.APPLICATION_JSON));
    }

    @Test
    public void whenPostCreated_includesXRequestedHeader() throws IOException {
        defineResource("headers", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                sentHeaders.put(X_REQUESTED_BY_HEADER, getHeader(X_REQUESTED_BY_HEADER));
                return new WebResource("", "text/plain");
            }
        });

        WebClient webClient = withWebClient("headers");
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasKey(X_REQUESTED_BY_HEADER));
    }

    @Test
    public void whenAuthorizationHeaderDefinedOnGet_sendIt() throws Exception {
        defineResource("headers", new PseudoServlet() {
            @Override
            public WebResource getGetResponse() {
                sentHeaders.put(AUTHENTICATION_HEADER, getHeader(AUTHENTICATION_HEADER));
                return new WebResource("", "text/plain");
            }
        });

        WebClient webClient = withWebClient("headers");
        webClient.setAuthentication("auth-value");
        webClient.doGetRequest();

        assertThat(sentHeaders, hasEntry(AUTHENTICATION_HEADER, "auth-value"));
    }

    @Test
    public void whenAuthorizationHeaderDefinedOnPost_sendIt() throws Exception {
        defineResource("headers", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                sentHeaders.put(AUTHENTICATION_HEADER, getHeader(AUTHENTICATION_HEADER));
                return new WebResource("", "text/plain");
            }
        });

        WebClient webClient = withWebClient("headers");
        webClient.setAuthentication("auth-value");
        webClient.doPostRequest("abced");

        assertThat(sentHeaders, hasEntry(AUTHENTICATION_HEADER, "auth-value"));
    }

    @Test
    public void returnValueFromServer() throws Exception {
        final String RESPONSE = "returned this";

        defineResource("query", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource(RESPONSE, "text/plain");
            }
        });

        WebClient webClient = withWebClient("query");
        assertThat(webClient.doPostRequest("abced"), equalTo(RESPONSE));
    }

    @Test
    public void when400StatusReceived_throwsRestQueryException() {
        defineResource("badRestQuery", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("bad query", "text/plain", SC_BAD_REQUEST);
            }
        });

        assertThrows(RestQueryException.class, () -> sendPostRequest("badRestQuery"));
    }

    private void sendPostRequest(String resourceName) throws IOException {
        withWebClient(resourceName).doPostRequest("abced");
    }

    @Test
    public void when401ReceivedFromServer_throwsException() {
        defineResource("protected", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                WebResource resource = new WebResource("unauthorized", "text/plain", SC_UNAUTHORIZED);
                resource.addHeader("WWW-Authenticate: Basic realm=\"REST Realm\"");
                return resource;
            }
        });

        assertThrows(AuthenticationChallengeException.class, () -> sendPostRequest("protected"));
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

        WebClient webClient = withWebClient("protected");

        try {
            webClient.doPostRequest("abcd");
        } catch (AuthenticationChallengeException e) {
            assertThat(extractRealm(e.getChallenge()), equalTo("REST Realm"));
        }
    }

    @Test
    public void when500StatusReceived_throwsServerErrorException() {
        defineResource("500Query", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("internal server error", "text/plain", SC_INTERNAL_SERVER_ERROR);
            }
        });

        assertThrows(ServerErrorException.class, () -> sendPostRequest("500Query"));
    }

    @Test
    public void when501StatusReceived_throwsServerErrorException() {
        defineResource("501Query", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("not implemented", "text/plain", SC_NOT_IMPLEMENTED);
            }
        });

        assertThrows(ServerErrorException.class, () -> sendPostRequest("501Query"));
    }

    @Test
    public void when502StatusReceived_throwsServerErrorException() {
        defineResource("502Query", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("bad gateway", "text/plain", SC_BAD_GATEWAY);
            }
        });

        assertThrows(ServerErrorException.class, () -> sendPostRequest("502Query"));
    }

    @Test
    public void when503StatusReceived_throwsServerErrorException() {
        defineResource("503Query", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("service unavailable", "text/plain", SC_SERVICE_UNAVAILABLE);
            }
        });

        assertThrows(ServerErrorException.class, () -> sendPostRequest("503Query"));
    }

    @Test
    public void when504StatusReceived_throwsServerErrorException() {
        defineResource("504Query", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("gateway timeout", "text/plain", SC_GATEWAY_TIMEOUT);
            }
        });

        assertThrows(ServerErrorException.class, () -> sendPostRequest("504Query"));
    }

    @Test
    public void when505StatusReceived_throwsServerErrorException() {
        defineResource("505Query", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("HTTP version not supported", "text/plain", SC_HTTP_VERSION_NOT_SUPPORTED);
            }
        });

        assertThrows(ServerErrorException.class, () -> sendPostRequest("505Query"));
    }

    // the value should be of the form <Basic realm="<realm-name>" and we want to extract the realm name
    private String extractRealm(String authenticationHeaderValue) {
        int start = authenticationHeaderValue.indexOf(QUOTE);
        int end = authenticationHeaderValue.indexOf(QUOTE, start+1);
        return start > 0 ? authenticationHeaderValue.substring(start+1, end) : "none";
    }

    @Test
    public void when403ReceivedFromServer_throwsException() {
        defineResource("forbidden", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("not allowed", "text/plain", SC_FORBIDDEN);
            }
        });

        assertThrows(ForbiddenException.class, () -> sendPostRequest("forbidden"));
    }

    @Test
    public void when404ReceivedFromServer_whatHappens() throws IOException {
        defineResource("missing", new PseudoServlet() {
            @Override
            public WebResource getPostResponse() {
                return new WebResource("not where I left it", "text/plain", SC_NOT_FOUND);
            }
        });

        try {
            sendPostRequest("abcde");
            fail("should have gotten an exception");
        } catch (ServerErrorException e) {
            assertThat(e.toString(), containsString("404"));
        }
    }

    @Test
    public void whenUnableToConnect_throwsException() {
        tearDownServer();

        assertThrows(RestPortConnectionException.class, () -> accessUndefinedPortRepeatedly("noConnection", 20));
    }

    @SuppressWarnings("SameParameterValue")
    private void accessUndefinedPortRepeatedly(String resourceName, int numTries) throws IOException {
        final WebClient webClient = withWebClient(resourceName);
        for (int attempt = 0; attempt < numTries; attempt++)
            accessUndefinedPort(webClient);
    }

    // try to read the just-closed port. If it is not yet closed, we get a socket exception, which we swallow to retry
    private void accessUndefinedPort(WebClient webClient) throws IOException {
        try {
            webClient.doPostRequest("abced");
        } catch (SocketException ignored) {
            // no-op
        }
    }
}
