package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import java.io.IOException;

import static com.meterware.simplestub.Stub.createStrictStub;

abstract class WebClientFactoryStub implements WebClientFactory {
    private WebClientStub client = createStrictStub(WebClientStub.class);
    private String clientURL;

    @Override
    public WebClient createClient(String clientURL) {
        this.clientURL = clientURL;
        return client;
    }

    void setException(WebClientException exception) {
        client.exception = exception;
    }

    void setResponse(String response) {
        client.response = response;
    }

    String getPostedString() {
        return client.postedValue;
    }

    String getClientURL() {
        return clientURL;
    }

    static abstract class WebClientStub extends WebClient {
        private WebClientException exception;
        private String response;
        private String postedValue;

        @Override
        void addHeader(String name, String value) {
            
        }

        @Override
        String getSetCookieHeader() {
            return null;
        }

        @Override
        String doGetRequest() {
            if (exception != null) throw exception;
            return response;
        }

        @Override
        String doPutRequest(String putBody) throws IOException {
            postedValue = putBody;
            return null;
        }
    }
}
