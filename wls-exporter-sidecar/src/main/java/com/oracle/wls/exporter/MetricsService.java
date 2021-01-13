package com.oracle.wls.exporter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

class MetricsService implements Service {

    private final InvocationContextFactory invocationContextFactory;
    private final WebClientFactory webClientFactory;
    private final ExecutorService executorService;

    MetricsService(InvocationContextFactory invocationContextFactory, WebClientFactory webClientFactory) {
        this.invocationContextFactory = invocationContextFactory;
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
        rules.get("/metrics", this::metrics);
    }

    private void metrics(ServerRequest req, ServerResponse res) {
        executorService.submit(() -> {
            try {
                InvocationContext context = invocationContextFactory.createContext(req, res);
                new ExporterCall(webClientFactory, context)
                        .doWithAuthentication();
            } catch (IOException e) {
                // unexpected failure on closing context or other I/O error - send 500 back
                res.send(e);
            }
        });
    }
}
