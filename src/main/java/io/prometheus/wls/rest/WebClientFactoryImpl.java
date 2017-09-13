package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @author Russell Gold
 */
public class WebClientFactoryImpl implements WebClientFactory {

    @Override
    public WebClient createClient(String url) {
        return new WebClientImpl(url);
    }
}
