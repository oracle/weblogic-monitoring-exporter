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
import java.util.Collection;
import java.util.Collections;

public class WebClientImpl implements WebClient {
    private String url;
    private String username;
    private String password;

    @Override
    public void initialize(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public String doQuery(String jsonQuery) throws IOException {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = createHttpClient(provider)) {
            HttpPost query = new HttpPost(url);
            query.setEntity(new StringEntity(jsonQuery, ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(query);
            if (response.getStatusLine().getStatusCode() == 400) throw new RestQueryException();
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

    private CloseableHttpClient createHttpClient(CredentialsProvider provider) {
        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .setDefaultHeaders(getDefaultHeaders())
                .build();
    }

    private Collection<? extends Header> getDefaultHeaders() {
        return Collections.singleton(new BasicHeader("X-Requested-By", "rest-exporter"));
    }
}
