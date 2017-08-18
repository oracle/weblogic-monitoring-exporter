package io.prometheus.wls.rest;

public class WebClientFactoryImpl implements WebClientFactory {
    private String username;
    private String password;

    @Override
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public WebClient createClient(String url) {
        return new WebClientImpl(url, username, password);
    }
}
