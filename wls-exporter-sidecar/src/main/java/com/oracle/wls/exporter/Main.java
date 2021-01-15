// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.concurrent.TimeUnit;

import io.helidon.common.LogConfig;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;


// use event variables to configure listen port and WLS port.

// what about SSL?
// what about initial configuration?
public class Main {
    public static void main(String[] args) {
        LogConfig.configureRuntime();
        SidecarConfiguration configuration = new SidecarConfiguration();

        WebServer.builder()
                .routing(Routing.builder()
                                 .register(new MetricsService(configuration, new WebClientFactoryImpl()))
                                 .build())
                .port(configuration.getListenPort())
                .build()
                .start()
                .await(10, TimeUnit.SECONDS);
    }
}
