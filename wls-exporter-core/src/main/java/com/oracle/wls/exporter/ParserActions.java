// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

public interface ParserActions {
  boolean isStart(String line);

  ParserState addHeader(String line);

  ParserState addDataLine(String line);

  boolean isEnd(String line);

  ParserState closeItem(ParserState state);
}
