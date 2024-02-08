// Copyright (c) 2021, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.io.IOException;
import java.util.function.BiFunction;

import com.oracle.wls.exporter.AuthenticatedCall;
import com.oracle.wls.exporter.ConfigurationDisplay;
import com.oracle.wls.exporter.ConfigurationPutCall;
import com.oracle.wls.exporter.ExporterCall;
import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.LiveConfiguration;
import com.oracle.wls.exporter.MessagesCall;
import com.oracle.wls.exporter.WebClientFactory;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;

class MetricsService implements HttpService {

    private final WebClientFactory webClientFactory;

    private final AuthenticatedHandler metricsHandler = new AuthenticatedHandler(ExporterCall::new);
    private final AuthenticatedHandler configurationHandler = new AuthenticatedHandler(ConfigurationPutCall::new);
    private final MainHandler mainHandler = new MainHandler();
    private final AuthenticatedHandler messagesHandler = new AuthenticatedHandler(MessagesCall::new);
    private final int listenPort;

    MetricsService(SidecarConfiguration configuration, WebClientFactory webClientFactory) {
        this.listenPort = configuration.getListenPort();
        this.webClientFactory = webClientFactory;
        LiveConfiguration.setServer(configuration.getWebLogicHost(), configuration.getWebLogicPort());
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/", mainHandler)
            .get("/metrics", metricsHandler)
            .get("/messages", messagesHandler)
            .put("/configuration", configurationHandler);
    }

    int getListenPort() {
        return listenPort;
    }

    abstract class MyHandler implements Handler {
        public void handle(ServerRequest request, ServerResponse response) {
            try {
                invoke(new HelidonInvocationContext(request, response));
            } catch (IOException e) {
                reportServerFailure(response, e);
            }
        }

        void reportServerFailure(ServerResponse response, IOException e) {
            response.send(e);
        }

        abstract void invoke(InvocationContext context) throws IOException;
    }

    class AuthenticatedHandler extends MyHandler {
        private final BiFunction<WebClientFactory, InvocationContext, AuthenticatedCall> builder;

        AuthenticatedHandler(BiFunction<WebClientFactory, InvocationContext, AuthenticatedCall> builder) {
            this.builder = builder;
        }

        @Override
        void invoke(InvocationContext context) throws IOException {
            builder.apply(webClientFactory, context).doWithAuthentication();
        }
    }

    class MainHandler extends MyHandler {
        void invoke(InvocationContext context) throws IOException {
            ConfigurationDisplay.displayConfiguration(context.getResponseStream());
            context.close();
        }
    }

}
