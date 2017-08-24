package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Thrown when the server sends an authentication challenge and the client doesn't have credentials.
 *
 * @author Russell Gold
 */
class BasicAuthenticationChallengeException extends RuntimeException {
    private String realm;

    BasicAuthenticationChallengeException(String realm) {
        this.realm = realm;
    }

    String getRealm() {
        return realm;
    }
}
