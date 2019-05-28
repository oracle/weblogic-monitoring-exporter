package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import static com.meterware.simplestub.Stub.createStrictStub;

abstract class WebClientFactoryStub implements WebClientFactory {
    private WebClientStub client = createStrictStub(WebClientStub.class);

    @Override
    public WebClient createClient() {
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
        return client.url;
    }

    static abstract class WebClientStub extends WebClient {
        private WebClientException exception;
        private String response;
        private String postedValue;
        private String url;

        @Override
        WebClient withUrl(String url) {
            this.url = url;
            return this;
        }

        @Override
        void addHeader(String name, String value) {
            
        }

        @Override
        String getSetCookieHeader() {
            return null;
        }

        @Override
        String doGetRequest() {
            if (url == null) throw new NullPointerException("No URL specified");
            if (exception != null) throw exception;
            return response;
        }

        @Override
        String doPutRequest(String putBody) {
            if (url == null) throw new NullPointerException("No URL specified");
            if (exception != null) throw exception;
            postedValue = putBody;
            return null;
        }
    }
}
