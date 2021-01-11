// Copyright (c) 2021, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.webapp;

import java.io.InputStream;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import com.oracle.wls.exporter.LiveConfiguration;

public class ServletUtils {

  /** The path to the configuration file within the web application. */
  public static final String CONFIG_YML = "/config.yml";

  /**
   * Specifies the server on which to contact the Management RESTful services.
   *
   * @param req the incoming request
   */
  public static void setServer(HttpServletRequest req) {
      LiveConfiguration.setServer(req.getServerName(), req.getServerPort());
  }

  /**
   * Loads the initial configuration during servlet load. Will skip the initialization if the configuration
   * has already been loaded from the config coordinator.
   *
   * @param servletConfig a standard servlet configuration which points to an exporter configuration
   */
  public static void initializeConfiguration(ServletConfig servletConfig) {
      LiveConfiguration.initialize(getConfigurationFile(servletConfig));
  }

  private static InputStream getConfigurationFile(ServletConfig config) {
      return config.getServletContext().getResourceAsStream(CONFIG_YML);
  }
}
