package io.prometheus.wls.rest;

/**
 * Thrown when the server sends an authentication challenge and the client doesn't have credentials.
 */
public class BasicAuthenticationChallengeException extends RuntimeException {
    private String realm;

    public BasicAuthenticationChallengeException(String realm) {
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }
}
