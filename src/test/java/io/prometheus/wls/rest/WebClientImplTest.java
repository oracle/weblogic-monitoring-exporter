package io.prometheus.wls.rest;

import com.meterware.httpunit.Base64;
import com.meterware.pseudoserver.HttpUserAgentTest;
import com.meterware.pseudoserver.PseudoServlet;
import com.meterware.pseudoserver.WebResource;
import org.junit.Before;
import org.junit.Test;

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

        webClient.initialize(getHostPath() + "/unprotected", "aa", "bb");
        webClient.doQuery(QUERY);

        assertThat(sentInfo, equalTo(QUERY));
    }

    @Test
    public void sendCredentialsWhenChallenged() throws Exception {
        defineResource("challenge", new PseudoServlet() {
            public WebResource getPostResponse() {
                String header = getHeader("Authorization");
                if (header == null) {
                    WebResource webResource = new WebResource("unauthorized", "text/plain", 401);
                    webResource.addHeader("WWW-Authenticate: Basic realm=\"testrealm\"");
                    return webResource;
                } else {
                    sentInfo = header;
                    return new WebResource("", "text/plain");
                }
            }
        });

        webClient.initialize(getHostPath() + "/challenge", "user", "password");
        webClient.doQuery("abced");

        assertThat(sentInfo, equalTo("Basic " + Base64.encode("user:password")));
    }

    @Test
    public void returnValueFromServer() throws Exception {
        final String RESPONSE = "returned this";

        defineResource("query", new PseudoServlet() {
            public WebResource getPostResponse() {
                return new WebResource(RESPONSE, "text/plain");
            }
        });

        webClient.initialize(getHostPath() + "/query", "user", "password");
        assertThat(webClient.doQuery("abced"), equalTo(RESPONSE));
    }

    
}