package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * The production implementation of the web client factory interface.
 *
 * @author Russell Gold
 */
public class WebClientFactoryImpl implements WebClientFactory {

    @Override
    public WebClient createClient() {
        return new WebClientImpl();
    }
}
