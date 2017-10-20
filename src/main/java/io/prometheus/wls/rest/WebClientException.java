package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

class WebClientException extends RuntimeException {
    WebClientException() {
    }

    WebClientException(Throwable cause, String message, Object... args) {
        super(formatMessage(message, args), cause);
    }

    private static String formatMessage(String message, Object... args) {
        return String.format(message, args);
    }
}
