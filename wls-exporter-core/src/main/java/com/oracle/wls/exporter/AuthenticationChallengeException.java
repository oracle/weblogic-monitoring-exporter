// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

/**
 * An exception indicating that the server has sent a challenge, demanding authentication.
 */
class AuthenticationChallengeException extends WebClientException {
    private String challenge;

    AuthenticationChallengeException(String challenge) {
        this.challenge = challenge;
    }

    String getChallenge() {
        return challenge;
    }
}
