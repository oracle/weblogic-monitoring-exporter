// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

import com.oracle.wls.exporter.domain.ExporterConfig;
import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

class MetricsService implements Service {

    private final WebClientFactory webClientFactory;
    private final ExecutorService executorService;

    private final Handler metricsHandler = new Handler(ExporterCall::new);
    private final Handler configurationHandler = new Handler(ConfigurationPutCall::new);

    MetricsService(SidecarConfiguration configuration, WebClientFactory webClientFactory) {
        ExporterConfig.setDefaultMetricsNameSnakeCase(true);
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
              .get("/metrics", metricsHandler::dispatch)
              .put("/configuration", configurationHandler::dispatch) ;
    }

    class Handler {
        private final BiFunction<WebClientFactory,InvocationContext,AuthenticatedCall> builder;

        Handler(BiFunction<WebClientFactory, InvocationContext, AuthenticatedCall> builder) {
            this.builder = builder;
        }

        void dispatch(ServerRequest request, ServerResponse response) {
            executorService.submit(() -> {
                try {
                    final InvocationContext context = new HelidonInvocationContext(request, response);
                    builder.apply(webClientFactory, context).doWithAuthentication();
                } catch (IOException e) {
                    reportServerFailure(response, e);
                }
            });
        }

        private void reportServerFailure(ServerResponse response, IOException e) {
            response.send(e);
        }
    }
}
