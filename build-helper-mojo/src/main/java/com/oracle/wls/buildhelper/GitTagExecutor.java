// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * An interface for the system interaction functionality required to execute the GitTag mojo.
 */
interface GitTagExecutor {

  /**
   * Issues a command and returns the response.
   * @param commandLine the command to execute
   */
  String runCommand(String... commandLine) throws IOException, MojoExecutionException;

  Path toPath(File file);
}
