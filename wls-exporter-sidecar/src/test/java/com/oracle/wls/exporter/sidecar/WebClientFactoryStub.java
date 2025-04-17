// Copyright (c) 2017, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.oracle.wls.exporter.AuthenticationChallengeException;
import com.oracle.wls.exporter.ForbiddenException;
import com.oracle.wls.exporter.WebClient;
import com.oracle.wls.exporter.WebClientCommon;
import com.oracle.wls.exporter.WebClientException;
import com.oracle.wls.exporter.WebClientFactory;

import static com.meterware.simplestub.Stub.createStrictStub;

public class WebClientFactoryStub implements WebClientFactory {
    private final WebClientStub webClient = createStrictStub(WebClientStub.class);

    @Override
    public WebClient createClient() {
        webClient.clearRetryNeeded();
        return webClient;
    }

    void addJsonResponse(Map<String, Object> responseMap) {
        webClient.addJsonResponse(new Gson().toJson(responseMap));
    }


    Map<String,String> getSentHeaders() {
        return webClient.sentHeaders;
    }

    String getSentAuthentication() {
        return webClient.getAuthentication();
    }

    public void reportNotAuthorized() {
        webClient.reportForbidden();
    }

    @SuppressWarnings("SameParameterValue")
    public void reportAuthenticationRequired(String basicRealmName) {
        webClient.reportAuthenticationRequired(basicRealmName);
    }

    abstract static class WebClientStub extends WebClientCommon {
        private static final String WLS_SEARCH_PATH = "/management/weblogic/latest/serverRuntime/search";

        private String url;
        private final List<TestResponse> testResponses = new ArrayList<>();
        private Iterator<TestResponse> responses;
        private final Map<String, String> addedHeaders = new HashMap<>();
        private Map<String, String> sentHeaders;

        private void addJsonResponse(String jsonResponse) {
            addResponse(new JsonResponse(jsonResponse));
        }

        private void addResponse(TestResponse response) {
            if (allResponsesHandled()) clearOldResponses();
            testResponses.add(response);
        }

        private boolean allResponsesHandled() {
            return responses != null && !responses.hasNext();
        }

        private void clearOldResponses() {
            testResponses.clear();
            responses = null;
        }

        void reportForbidden() {
            addExceptionResponse(new ForbiddenException());
        }

        private void addExceptionResponse(WebClientException e) {
            addResponse(new ExceptionResponse(e));
        }

        @SuppressWarnings("SameParameterValue")
        void reportAuthenticationRequired(String basicRealmName) {
            addExceptionResponse(new AuthenticationChallengeException(String.format("Basic realm=\"%s\"", basicRealmName)));
        }

        @Override
        public WebClientCommon withUrl(String url) {
            this.url = url;
            return this;
        }

        @Override
        public void addHeader(String name, String value) {
            addedHeaders.put(name, value);
        }

        @Override
        public String doPostRequest(String postBody) {
            if (url == null) throw new NullPointerException("No URL specified");
            sentHeaders = Collections.unmodifiableMap(addedHeaders);

            return getResult(getNextResponse());
        }

        @Override
        public String doGetRequest() {
            if (url == null) throw new NullPointerException("No URL specified");
            if (url.contains(WLS_SEARCH_PATH)) throw new AssertionError("GET to search paths is not supported");

            return getResult(getNextResponse());
        }

        private TestResponse getNextResponse() {
            if (responses == null) responses = testResponses.iterator();
            return !responses.hasNext() ? new JsonResponse(null) : responses.next();
        }

        private String getResult(TestResponse response) {
            if (response.getException() != null) throw response.getException();
            return response.getJsonResponse();
        }

    }

    interface TestResponse {
        WebClientException getException();
        String getJsonResponse();
    }

    static class ExceptionResponse implements TestResponse {
        private final WebClientException webClientException;

        ExceptionResponse(WebClientException webClientException) {
            this.webClientException = webClientException;
        }

        @Override
        public WebClientException getException() {
            return webClientException;
        }

        @Override
        public String getJsonResponse() {
            return null;
        }
    }

    static class JsonResponse implements TestResponse {
        String jsonResponse;

        JsonResponse(String jsonResponse) {
            this.jsonResponse = jsonResponse;
        }

        @Override
        public WebClientException getException() {
            return null;
        }

        @Override
        public String getJsonResponse() {
            return jsonResponse;
        }
    }
}
