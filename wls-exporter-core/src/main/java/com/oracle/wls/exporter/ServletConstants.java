// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

/**
 * Constants needed by multiple servlets.
 * 
 * @author Russell Gold
 */
public interface ServletConstants {
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

    /** The header used by a web client to send its authentication credentials. **/
    String AUTHENTICATION_HEADER = "Authorization";

    /** The header used by a web client to specify the content type of its data. **/
    String CONTENT_TYPE_HEADER = "Content-Type";

    /** The header used by a web client to send cookies as part of a request. */
    String COOKIE_HEADER = "Cookie";

    // The field which defines the configuration update action
    String EFFECT_OPTION = "effect";

    // The possible values for the effect
    String DEFAULT_ACTION = ServletConstants.REPLACE_ACTION;
    String REPLACE_ACTION = "replace";
    String APPEND_ACTION = "append";
}
