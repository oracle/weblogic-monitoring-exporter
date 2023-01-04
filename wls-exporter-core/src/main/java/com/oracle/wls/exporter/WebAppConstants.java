// Copyright (c) 2017, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

/**
 * Constants needed by multiple servlets.
 * 
 * @author Russell Gold
 */
public interface WebAppConstants {
    String PAGE_HEADER
          = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Weblogic Monitoring Exporter</title>\n" +
            "</head>\n" +
            "<body>";

    // The locations of the servlets relative to the web app
    String MAIN_PAGE = "";
    String METRICS_PAGE = "metrics";
    String CONFIGURATION_PAGE = "configure";
    String MESSAGES_PAGE = "messages";
    String LOG_PAGE = "log";

    /** The header sent by a web server to require authentication. **/
    String AUTHENTICATION_CHALLENGE_HEADER = "WWW-Authenticate";

    /** The header used by a web server to define a new cookie. **/
    String SET_COOKIE_HEADER = "Set-Cookie";

    /** The header used by a web client to send its authentication credentials. **/
    String AUTHENTICATION_HEADER = "Authorization";

    /** The header used by a web client to pass a created cookie to a server. **/
    String COOKIE_HEADER = "Cookie";

    /** The header used by a web client to specify the content type of its data. **/
    String CONTENT_TYPE_HEADER = "Content-Type";

    // The field which defines the configuration update action
    String EFFECT_OPTION = "effect";

    // The possible values for the effect
    String REPLACE_ACTION = "replace";
    String APPEND_ACTION = "append";
    String DEFAULT_ACTION = REPLACE_ACTION;
}
