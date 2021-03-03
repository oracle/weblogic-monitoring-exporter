// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

/**
 * An exception indicating that the server has sent a challenge, demanding authentication.
 */
public class AuthenticationChallengeException extends WebClientException {
    private final String challenge;

    public AuthenticationChallengeException(String challenge) {
        this.challenge = challenge;
    }

    String getChallenge() {
        return challenge;
    }
}
