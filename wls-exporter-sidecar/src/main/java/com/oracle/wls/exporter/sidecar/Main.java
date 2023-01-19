// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import java.util.concurrent.TimeUnit;

import com.oracle.wls.exporter.WebClientFactoryImpl;
import io.helidon.common.LogConfig;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;


// Main program for the Exporter sidecar using Helidon
public class Main {
    public static void main(String[] args) {
        LogConfig.configureRuntime();
        final SidecarConfiguration configuration = new SidecarConfiguration();
        final MetricsService metricsService = new MetricsService(configuration, new WebClientFactoryImpl());

        WebServer.builder()
                .addRouting(Routing.builder().register(metricsService).build())
                .port(metricsService.getListenPort())
                .build()
                .start()
                .await(10, TimeUnit.SECONDS);
    }
}
