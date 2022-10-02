// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.*;

import static com.meterware.simplestub.Stub.createStrictStub;

public class WebClientFactoryStub implements WebClientFactory {
    private final WebClientStub webClient = createStrictStub(WebClientStub.class);
    private int queryIndex = 0;

    @Override
    public WebClient createClient() {
        webClient.clearRetryNeeded();
        return webClient;
    }

    void addJsonResponse(Map<String, Object> responseMap) {
        webClient.addJsonResponse(new Gson().toJson(responseMap));
    }

    void addJsonResponse(String json) {
        webClient.addJsonResponse(json);
    }

    int getNumQueriesSent() {
        return webClient.jsonQueries.size();
    }

    String getSentQuery() {
        if (webClient.jsonQueries.size() <= queryIndex)
            throw new WebClientException(
                  "Attempted to read query %n but only %n were sent", queryIndex+1, getNumQueriesSent());

        return webClient.jsonQueries.get(queryIndex++);
    }

    String getPostedString() {
        return webClient.postedString;
    }

    Map<String,String> getSentHeaders() {
        return webClient.sentHeaders;
    }

    String getSentAuthentication() {
        return webClient.getAuthentication();
    }

    public String getClientUrl() {
        return webClient.url;
    }

    public void reportNotAuthorized() {
        webClient.reportForbidden();
    }

    void reportBadQuery() {
        webClient.reportBadQuery();
    }

    @SuppressWarnings("SameParameterValue")
    public void reportAuthenticationRequired(String basicRealmName) {
        webClient.reportAuthenticationRequired(basicRealmName);
    }

    @SuppressWarnings("SameParameterValue")
    public void throwConnectionFailure(String hostName, int port) {
        webClient.throwConnectionFailure(hostName, port);
    }

    void throwWebClientException(WebClientException e) {
        webClient.addExceptionResponse(e);
    }

    static abstract class WebClientStub extends WebClientCommon {
        private final static String WLS_SEARCH_PATH = "/management/weblogic/latest/serverRuntime/search";

        private String url;
        private final List<String> jsonQueries = new ArrayList<>();
        private final List<TestResponse> testResponses = new ArrayList<>();
        private Iterator<TestResponse> responses;
        private final Map<String, String> addedHeaders = new HashMap<>();
        private Map<String, String> sentHeaders;
        private String postedString;

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

        void reportBadQuery() {
            addExceptionResponse(new RestQueryException());
        }

        @SuppressWarnings("SameParameterValue")
        void reportAuthenticationRequired(String basicRealmName) {
            addExceptionResponse(new AuthenticationChallengeException(String.format("Basic realm=\"%s\"", basicRealmName)));
        }

        void throwConnectionFailure(String hostName, int port) {
            addExceptionResponse(new RestPortConnectionException(String.format("http://%s:%d", hostName, port)));
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
            this.jsonQueries.add(postBody);

            return getResult(getNextResponse());
        }

        @Override
        public String doGetRequest() {
            if (url == null) throw new NullPointerException("No URL specified");
            if (url.contains(WLS_SEARCH_PATH)) throw new AssertionError("GET to search paths is not supported");

            return getResult(getNextResponse());
        }

        @Override
        public <T> String doPutRequest(T putBody) throws IOException {
            if (url == null) throw new NullPointerException("No URL specified");
            postedString = new Gson().toJson(putBody);

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

    static class QueryTestException extends WebClientException {
        public QueryTestException(String message) {
            super(message);
        }
    }
}
