// Copyright (c) 2021, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.UrlBuilder;
import com.oracle.wls.exporter.WebAppConstants;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class HelidonInvocationContext implements InvocationContext {

    private final ServerRequest request;
    private final ServerResponse response;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    private final PrintStream printStream = new PrintStream(baos);
    private final SidecarConfiguration configuration = new SidecarConfiguration();

    private Map<String, String> cookies;

    public HelidonInvocationContext(ServerRequest request, ServerResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public UrlBuilder createUrlBuilder() {
        return UrlBuilder.create(configuration.useWebLogicSsl())
              .withHostName(configuration.getWebLogicHost())
              .withPort(configuration.getWebLogicPort());
    }

    @Override
    public String getApplicationContext() {
        return "/";
    }

    @Override
    public String getAuthenticationHeader() {
        return request.headers().first(WebAppConstants.AUTHENTICATION_HEADER).orElse(null);
    }

    @Override
    public String getContentType() {
        return request.headers().contentType().map(MediaType::toString).orElse("application/json");
    }

    @Override
    public Map<String, String> getCookies() {
        if (cookies == null)
            cookies = getRequestCookies();

        return cookies;
    }

    private Map<String, String> getRequestCookies() {
        return request.headers().all("Cookie").stream()
              .map(Cookie::new)
              .collect(Collectors.toMap(Cookie::getName, Cookie::getValue, this::keepFirst));
    }

    private static class Cookie {
        final String name;
        final String value;

        Cookie(String cookieHeader) {
            final String[] header = cookieHeader.split(";");
            final String[] cookie = header[0].split("=", 2);
            name = cookie[0];
            value = cookie[1];
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }
    }

    private String keepFirst(String first, String second) {
        return first;
    }

    String[] splitCookie(String cookieHeader) {
        final String[] header = cookieHeader.split(";");
        return header[0].split("=", 2);
    }

    @Override
    public String getInstanceName() {
        return configuration.getPodName();
    }

    @Override
    public InputStream getRequestStream() {
        return request.content().as(String.class)
                .thenApply(String::getBytes)
                .thenApply(ByteArrayInputStream::new)
                .await(10, TimeUnit.SECONDS);
    }

    @Override
    public PrintStream getResponseStream() {
        return printStream;
    }

    @Override
    public void sendError(int status, String msg) {
        response.status(status).send(msg);
    }

    @Override
    public void sendRedirect(String location) {
        response.headers().location(URI.create(location));

        response.status(Http.Status.FOUND_302).send();
    }

    @Override
    public void addResponseHeader(String name, String value) {
        response.headers().add(name, value);
    }

    @Override
    public void setStatus(int status) {
        response.status(status);
    }

    @Override
    public void close() {
        response.send(Single.just(DataChunk.create(baos.toByteArray())));
    }
}
