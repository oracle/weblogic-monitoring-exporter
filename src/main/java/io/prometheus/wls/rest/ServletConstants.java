package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @author Russell Gold
 */
public interface ServletConstants {
    String PAGE_HEADER
          = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Weblogic Prometheus Exporter</title>\n" +
            "</head>\n" +
            "<body>";

    String MAIN_PAGE = "";
    String CONFIGURATION_ACTION = "configure";

    /** The header used by a web client to send its authentication credentials. **/
    String AUTHENTICATION_HEADER = "Authorization";

    /** The header used by a web client to send cookies as part of a request. */
    String COOKIE_HEADER = "Cookie";
}
