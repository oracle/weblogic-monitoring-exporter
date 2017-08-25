package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * These are the HTTP status codes returned by a web server.
 *
 * @author Russell Gold
 */
public interface ServletConstants {
    int SUCCESS                 = 200;

    int BAD_REQUEST             = 400;
    int AUTHENTICATION_REQUIRED = 401;
    int NOT_AUTHORIZED          = 403;

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

}
