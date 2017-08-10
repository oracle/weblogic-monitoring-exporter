package io.prometheus.wls.rest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.prometheus.wls.rest.StatusCodes.AUTHENTICATION_REQUIRED;
import static io.prometheus.wls.rest.StatusCodes.BAD_REQUEST;
import static io.prometheus.wls.rest.StatusCodes.NOT_AUTHORIZED;

public class WebClientImpl implements WebClient {
    private static final char QUOTE = '"';

    private String url;
    private String username;
    private String password;
    private String credentials;

    @Override
    public void initialize(String url) {
        this.url = url;
    }

    @Override
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void setAuthenticationCredentials(String credentials) {
        this.credentials = credentials;
    }

    @Override
    public String doQuery(String jsonQuery) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost query = new HttpPost(url);
            query.setEntity(new StringEntity(jsonQuery, ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(query);
            processStatusCode(response);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                try (InputStream inputStream = responseEntity.getContent()) {
                    byte[] buffer = new byte[4096];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int numBytes = 0;
                    while (numBytes >= 0) {
                        baos.write(buffer, 0, numBytes);
                        numBytes = inputStream.read(buffer);
                    }
                    return baos.toString("UTF-8");
                }
            }
        }
        return null;
    }

    private void processStatusCode(CloseableHttpResponse response) throws RestQueryException {
        switch (response.getStatusLine().getStatusCode()) {
            case BAD_REQUEST:
                throw new RestQueryException();
            case AUTHENTICATION_REQUIRED:
                throw createAuthenticationChallengeException(response);
            case NOT_AUTHORIZED:
                throw new NotAuthorizedException();
        }
    }

    private BasicAuthenticationChallengeException createAuthenticationChallengeException(CloseableHttpResponse response) {
        return new BasicAuthenticationChallengeException(getRealm(response));
    }

    private String getRealm(CloseableHttpResponse response) {
        Header header = response.getFirstHeader("WWW-Authenticate");
        return extractRealm(header == null ? "" : header.getValue());
    }

    // the value should be of the form <Basic realm="<realm-name>" and we want to extract the realm name
    private String extractRealm(String authenticationHeaderValue) {
        int start = authenticationHeaderValue.indexOf(QUOTE);
        int end = authenticationHeaderValue.indexOf(QUOTE, start+1);
        return start > 0 ? authenticationHeaderValue.substring(start+1, end) : "none";
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(getCredentialsProvider())
                .setDefaultHeaders(getDefaultHeaders())
                .build();
    }

    private CredentialsProvider getCredentialsProvider() {
        if (useUsernamePassword()) {
            return createCredentialsProvider(username, password);
        } else {
            return null;
        }
    }

    private boolean useUsernamePassword() {
        return credentials == null && username != null && password != null;
    }

    private static CredentialsProvider createCredentialsProvider(String username, String password) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }

    private Collection<? extends Header> getDefaultHeaders() {
        List<Header> headers = new ArrayList<>(Collections.singleton(new BasicHeader("X-Requested-By", "rest-exporter")));
        if (credentials != null)
            headers.add(new BasicHeader("Authorization", credentials));
        return headers;
    }
}
