// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface CopyExecutor {

  Path toPath(File file);

  void copyFile(Path sourcePath, Path targetPath) throws IOException;

}
