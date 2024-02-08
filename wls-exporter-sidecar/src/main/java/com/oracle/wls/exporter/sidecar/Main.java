// Copyright (c) 2021, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.sidecar;

import com.oracle.wls.exporter.WebClientFactoryImpl;
import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;


// Main program for the Exporter sidecar using Helidon
public class Main {
    public static void main(String[] args) {
        LogConfig.configureRuntime();
        final SidecarConfiguration configuration = new SidecarConfiguration();
        final MetricsService metricsService = new MetricsService(configuration, new WebClientFactoryImpl());

        WebServer.builder()
                .addRouting(HttpRouting.builder().register(metricsService))
                .port(metricsService.getListenPort())
                .build()
                .start();
    }
}
