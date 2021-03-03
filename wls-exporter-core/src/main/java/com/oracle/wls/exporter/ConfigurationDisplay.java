// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.OutputStream;
import java.io.PrintStream;

public class ConfigurationDisplay {

  public static void displayConfiguration(OutputStream outputStream) {
      try (PrintStream ps = new PrintStream(outputStream)) {
          ps.println("<p>Current Configuration</p>");
          ps.println("<p><code><pre>");
          ps.print(LiveConfiguration.asString());
          ps.println("</pre></code></p>");
      }
  }
}
