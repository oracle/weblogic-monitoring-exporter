package io.prometheus.wls.rest;

/**
 * Thrown when the server sends an authentication challenge and the client doesn't have credentials.
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
