package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

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
    String LOG_PAGE = "log";

    /** The header used by a web client to send its authentication credentials. **/
    String AUTHENTICATION_HEADER = "Authorization";

    /** The header used by a web client to send cookies as part of a request. */
    String COOKIE_HEADER = "Cookie";

    // The field which defines the configuration update action
    String EFFECT_OPTION = "effect";

    // The possible values for the effect
    String DEFAULT_ACTION = ServletConstants.REPLACE_ACTION;
    String REPLACE_ACTION = "replace";
    String APPEND_ACTION = "append";
}
