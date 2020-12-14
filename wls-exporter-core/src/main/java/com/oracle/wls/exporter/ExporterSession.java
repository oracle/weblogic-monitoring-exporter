// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the session behavior of the exporter.
 *
 * @author Russell Gold
 */
class ExporterSession {
    static final String SESSION_COOKIE_PREFIX = "JSESSIONID=";

    private static final String SESSION_ID_REGEX = "[!-+--:<-~]+";
    private static final String SESSION_COOKIE_REGEX = ".*(" + SESSION_COOKIE_PREFIX + SESSION_ID_REGEX + ").*";
    private static final Pattern SESSION_COOKIE_PATTERN = Pattern.compile(SESSION_COOKIE_REGEX);

    /** The authentication string currently used to access the REST API. **/
    private static String authentication;

    /** The REST API session cookie. Consists of the prefix plus a session ID. **/
    private static String sessionCookie;

    static String getSessionCookie(String headerValue) {
        Matcher matcher = SESSION_COOKIE_PATTERN.matcher(headerValue);
        return matcher.matches() ? matcher.group(1) : null;
    }

    static void cacheSession(String authentication, String sessionCookie) {
        ExporterSession.authentication = authentication;
        ExporterSession.sessionCookie = sessionCookie;
    }

    static String getAuthentication() {
        return authentication;
    }

    static String getSessionCookie() {
        return sessionCookie;
    }

}
