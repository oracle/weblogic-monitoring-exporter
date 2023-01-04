// Copyright (c) 2017, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.oracle.wls.exporter.WebAppConstants.SET_COOKIE_HEADER;

public class WebClientFactoryStub implements WebClientFactory {
    private final WebClientStub webClient = createStrictStub(WebClientStub.class);
    private int queryIndex = 0;

    @Override
    public WebClient createClient() {
        webClient.clearRetryNeeded();
        return webClient;
    }

    interface ResponseBuilder {
        ResponseBuilder withResponseHeader(String name, String value);
        void addResponse();
    }

    void addJsonResponse(Map<String, Object> responseMap) {
        webClient.addJsonResponse(new Gson().toJson(responseMap));
    }

    void addJsonResponse(String json) {
        webClient.addJsonResponse(json);
    }

    ResponseBuilder forJson(String json) {
        return webClient.forJson(json);
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

    List<String> getSentHeaders(String headerName) {
        return Optional.ofNullable(webClient.sentHeaders.get(headerName)).orElse(Collections.emptyList());
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
        private final Map<String, List<String>> addedHeaders = new HashMap<>();
        private Map<String, List<String>> sentHeaders;
        private String postedString;

        private void addJsonResponse(String responseJson) {
            addResponse(new JsonResponse(responseJson));
        }

        private void addResponse(TestResponse response) {
            if (allResponsesHandled()) clearOldResponses();
            testResponses.add(response);
        }

        private ResponseBuilder forJson(String jsonString) {
            return new ResponseBuilderImpl(jsonString);
        }

        class ResponseBuilderImpl implements ResponseBuilder {
            private final JsonResponse jsonResponse;

            ResponseBuilderImpl(String jsonString) {
                this.jsonResponse = new JsonResponse(jsonString);
                WebClientStub.this.addedHeaders.clear();
            }

            @Override
            public ResponseBuilder withResponseHeader(String name, String value) {
                jsonResponse.withResponseHeader(name, value);
                return this;
            }

            @Override
            public void addResponse() {
                WebClientStub.this.addResponse(jsonResponse);
            }
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
            addExceptionResponse(new RestPortConnectionException(String.format("https://%s:%d", hostName, port)));
        }

        @Override
        public WebClientCommon withUrl(String url) {
            this.url = url;
            return this;
        }

        @Override
        public void addHeader(String name, String value) {
            addedHeaders.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
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
        public <T> String doPutRequest(T putBody) {
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

            invokeSetCookieHandlerCallbacks(getSetCookieHeaders(response));
            return response.getJsonResponse();
        }

        private List<String> getSetCookieHeaders(TestResponse response) {
            return response.getResponseHeaders(SET_COOKIE_HEADER);
        }

    }

    interface TestResponse {
        WebClientException getException();
        String getJsonResponse();
        List<String> getResponseHeaders(String headerName);
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

        @Override
        public List<String> getResponseHeaders(String headerName) {
            return Collections.emptyList();
        }
    }

    static class JsonResponse implements TestResponse {
        String jsonResponse;
        private final Map<String, List<String>> responseHeaders = new HashMap<>();

        JsonResponse(String jsonResponse) {
            this.jsonResponse = jsonResponse;
        }

        JsonResponse withResponseHeader(String name, String value) {
            getResponseHeaders(name).add(value);
            return this;
        }

        @Override
        public WebClientException getException() {
            return null;
        }

        @Override
        public String getJsonResponse() {
            return jsonResponse;
        }

        @Override
        public List<String> getResponseHeaders(String headerName) {
            return responseHeaders.computeIfAbsent(headerName, k -> new ArrayList<>());
        }
    }

}
