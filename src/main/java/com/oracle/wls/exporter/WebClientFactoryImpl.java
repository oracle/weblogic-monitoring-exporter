// Copyright (c) 2017, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

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
