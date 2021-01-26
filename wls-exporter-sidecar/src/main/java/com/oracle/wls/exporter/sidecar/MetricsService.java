// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

import com.oracle.wls.exporter.AuthenticatedCall;
import com.oracle.wls.exporter.ConfigurationDisplay;
import com.oracle.wls.exporter.ConfigurationPutCall;
import com.oracle.wls.exporter.ExporterCall;
import com.oracle.wls.exporter.InvocationContext;
import com.oracle.wls.exporter.LiveConfiguration;
import com.oracle.wls.exporter.WebClientFactory;
import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

class MetricsService implements Service {

    private final WebClientFactory webClientFactory;
    private final ExecutorService executorService;

    private final AuthenticatedHandler metricsHandler = new AuthenticatedHandler(ExporterCall::new);
    private final AuthenticatedHandler configurationHandler = new AuthenticatedHandler(ConfigurationPutCall::new);
    private final MainHandler mainHandler = new MainHandler();

    MetricsService(SidecarConfiguration configuration, WebClientFactory webClientFactory) {
        LiveConfiguration.setServer(configuration.getWebLogicHost(), configuration.getWebLogicPort());
        this.webClientFactory = webClientFactory;

        this.executorService = ThreadPoolSupplier.builder()
                .threadNamePrefix("wls-exporter-sidecar-")
                .corePoolSize(10)
                .prestart(true)
                .build()
                .get();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
              .get("/", mainHandler::dispatch)
              .get("/metrics", metricsHandler::dispatch)
              .put("/configuration", configurationHandler::dispatch);
    }

    abstract class Handler {
        void dispatch(ServerRequest request, ServerResponse response) {
            executorService.submit(() -> {
                try {
                    invoke(new HelidonInvocationContext(request, response));
                } catch (IOException e) {
                    reportServerFailure(response, e);
                }
            });
        }

        void reportServerFailure(ServerResponse response, IOException e) {
            response.send(e);
        }

        abstract void invoke(InvocationContext context) throws IOException;
    }

    class AuthenticatedHandler extends Handler {
        private final BiFunction<WebClientFactory, InvocationContext, AuthenticatedCall> builder;

        AuthenticatedHandler(BiFunction<WebClientFactory, InvocationContext, AuthenticatedCall> builder) {
            this.builder = builder;
        }

        @Override
        void invoke(InvocationContext context) throws IOException {
            builder.apply(webClientFactory, context).doWithAuthentication();
        }
    }

    class MainHandler extends Handler {
        void invoke(InvocationContext context) throws IOException {
            ConfigurationDisplay.displayConfiguration(context.getResponseStream());
        }
    }
}
