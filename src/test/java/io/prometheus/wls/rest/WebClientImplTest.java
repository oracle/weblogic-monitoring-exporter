package io.prometheus.wls.rest;

import com.meterware.httpunit.Base64;
import com.meterware.pseudoserver.HttpUserAgentTest;
import com.meterware.pseudoserver.PseudoServlet;
import com.meterware.pseudoserver.WebResource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static io.prometheus.wls.rest.StatusCodes.AUTHENTICATION_REQUIRED;
import static io.prometheus.wls.rest.StatusCodes.BAD_REQUEST;
import static io.prometheus.wls.rest.StatusCodes.NOT_AUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class WebClientImplTest extends HttpUserAgentTest {

    private WebClient webClient = new WebClientImpl();

    private static String sentInfo;

    @Before
    public void setUp() throws Exception {
        sentInfo = null;
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

        webClient.defineQueryUrl(getHostPath() + "/unprotected");
        webClient.doQuery(QUERY);

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

        webClient.defineQueryUrl(getHostPath() + "/checkXRequested");
        webClient.doQuery("abced");
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
                    sentInfo = header;
                    return new WebResource("", "text/plain");
                }
            }
        });

        webClient.defineQueryUrl(getHostPath() + "/challenge");
        webClient.setCredentials("user", "password");
        webClient.doQuery("abced");

        assertThat(sentInfo, equalTo("Basic " + Base64.encode("user:password")));
    }

    @Test
    public void whenPreemptiveAuthenticationDefined_sendAuthentication() throws Exception {
        defineResource("preemptive", new PseudoServlet() {
            public WebResource getPostResponse() {
                String header = getHeader("Authorization");
                if (header == null) {
                    return new WebResource("not allowed", "text/plain", NOT_AUTHORIZED);
                } else {
                    sentInfo = header;
                    return new WebResource("", "text/plain");
                }
            }
        });

        String authentication = "Basic " + Base64.encode("user:password");

        webClient.defineQueryUrl(getHostPath() + "/preemptive");
        webClient.setAuthenticationCredentials(authentication);
        webClient.doQuery("abced");

        assertThat(sentInfo, equalTo(authentication));
    }

    @Test
    public void returnValueFromServer() throws Exception {
        final String RESPONSE = "returned this";

        defineResource("query", new PseudoServlet() {
            public WebResource getPostResponse() {
                return new WebResource(RESPONSE, "text/plain");
            }
        });

        webClient.defineQueryUrl(getHostPath() + "/query");
        webClient.setCredentials("user", "password");
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

        webClient.defineQueryUrl(getHostPath() + "/badRestQuery");
        webClient.doQuery("abcd");
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

        webClient.defineQueryUrl(getHostPath() + "/protected");
        webClient.doQuery("abcd");
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

        webClient.defineQueryUrl(getHostPath() + "/protected");

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

        webClient.defineQueryUrl(getHostPath() + "/forbidden");
        webClient.doQuery("abcd");
    }
}