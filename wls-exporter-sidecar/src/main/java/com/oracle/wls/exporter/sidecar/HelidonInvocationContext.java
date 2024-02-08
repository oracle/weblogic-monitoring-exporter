// Copyright (c) 2021, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;

import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.UrlBuilder;
import io.helidon.http.HeaderNames;
import io.helidon.common.media.type.MediaType;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class HelidonInvocationContext implements InvocationContext {

    private final ServerRequest request;
    private final ServerResponse response;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    private final PrintStream printStream = new PrintStream(baos);
    private final SidecarConfiguration configuration = new SidecarConfiguration();

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
        return request.headers().first(HeaderNames.AUTHORIZATION).orElse(null);
    }

    @Override
    public String getContentType() {
        return request.headers().contentType().map(MediaType::toString).orElse("application/json");
    }

    @Override
    public String getInstanceName() {
        return configuration.getPodName();
    }

    @Override
    public InputStream getRequestStream() {
        return request.content().inputStream();
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

        response.status(Status.FOUND_302).send();
    }

    @Override
    public void setResponseHeader(String name, String value) {
        response.headers().add(HeaderNames.create(name), value);
    }

    @Override
    public void setStatus(int status) {
        response.status(status);
    }

    @Override
    public void close() {
        if (!response.isSent()) {
            response.send(baos.toByteArray());
        }
    }
}
