package com.oracle.wls.exporter;

import java.util.concurrent.TimeUnit;

import io.helidon.common.LogConfig;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

public class Main {
    public static void main(String[] args) {
        LogConfig.configureRuntime();

        WebServer.builder()
                .routing(Routing.builder()
                                 .register(new MetricsService(HelidonInvocationContextFactory.create(),
                                                              new WebClientFactoryImpl()))
                                 .build())
                .port(7004)
                .build()
                .start()
                .await(10, TimeUnit.SECONDS);
    }
}
