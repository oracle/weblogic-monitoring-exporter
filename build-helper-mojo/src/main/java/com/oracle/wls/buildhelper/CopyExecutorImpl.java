// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import java.io.File;
import java.nio.file.Path;

public class CopyExecutorImpl implements CopyExecutor {

  @Override
  public Path toPath(File file) {
    return file.toPath();
  }

}
