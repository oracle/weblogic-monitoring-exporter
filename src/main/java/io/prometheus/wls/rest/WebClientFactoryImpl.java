package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @author Russell Gold
 */
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
