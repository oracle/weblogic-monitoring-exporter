// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

public enum ParserState {
  INITIAL {
    @Override
    ParserState processLine(String line, ParserActions actions) {
      return actions.isStart(line) ? HEADERS : this;
    }
  },
  HEADERS {
    @Override
    ParserState processLine(String line, ParserActions actions) {
      if (line.isEmpty()) {
        return CONTENT;
      } else {
        return actions.addHeader(line);
      }
    }
  },
  CONTENT {
    @Override
    ParserState processLine(String line, ParserActions actions) {
      if (actions.isEnd(line))
        return actions.closeItem(DONE);
      else if (actions.isStart(line))
        return actions.closeItem(HEADERS);
      else
        return actions.addDataLine(line);
    }
  },
  DONE;

  ParserState processLine(String line, ParserActions actions) {
    return this;
  }
}
