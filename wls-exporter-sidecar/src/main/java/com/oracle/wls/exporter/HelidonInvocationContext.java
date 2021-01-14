package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class HelidonInvocationContext implements InvocationContext {

    private final ServerRequest req;
    private final ServerResponse res;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    private final PrintStream printStream = new PrintStream(baos);

    public HelidonInvocationContext(ServerRequest req, ServerResponse res) {
        this.req = req;
        this.res = res;
    }

    @Override
    public UrlBuilder createUrlBuilder() {
        return UrlBuilder.create(req.localAddress(), req.isSecure())
                .withPort(LiveConfiguration.getConfiguredRestPort())
                .withPort(req.webServer().port());
    }

    @Override
    public String getApplicationContext() {
        return req.path().toRawString();
    }

    @Override
    public String getAuthenticationHeader() {
        return req.headers().first(WebAppConstants.AUTHENTICATION_HEADER).orElse(null);
    }

    @Override
    public String getContentType() {
        return req.headers().contentType().map(MediaType::toString).orElse("application/json");
    }

    @Override
    public String getInstanceName() {
        return req.localAddress() + ":" + req.webServer().port();
    }

    @Override
    public InputStream getRequestStream() {
        return req.content().as(String.class)
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
        res.status(status).send(msg);
    }

    @Override
    public void sendRedirect(String location) {
        res.headers().location(URI.create(location));

        res.status(Http.Status.FOUND_302).send();
    }

    @Override
    public void setResponseHeader(String name, String value) {
        res.headers().add(name, value);
    }

    @Override
    public void setStatus(int status) {
        res.status(status);
    }

    @Override
    public void close() {
        res.send(Single.just(DataChunk.create(baos.toByteArray())));
    }
}
