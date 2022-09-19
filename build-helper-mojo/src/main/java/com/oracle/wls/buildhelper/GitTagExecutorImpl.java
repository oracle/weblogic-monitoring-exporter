// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class GitTagExecutorImpl implements GitTagExecutor {
  @Override
  public String runCommand(String... commandLineStrings) throws IOException, InterruptedException {
    /**/
    Process vp = new ProcessBuilder(commandLineStrings).start();
    /**/

    vp.waitFor();

    final BufferedReader br = new BufferedReader(new InputStreamReader(vp.getInputStream()));
    return br.lines().collect(Collectors.joining("\n"));
  }

  @Override
  public Path toPath(File file) {
    return file.toPath();
  }
}
