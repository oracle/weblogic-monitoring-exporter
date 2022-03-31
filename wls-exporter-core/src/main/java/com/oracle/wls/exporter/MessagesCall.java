// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.PrintStream;

public class MessagesCall extends AuthenticatedCall {

  public MessagesCall(WebClientFactory webClientFactory, InvocationContext context) {
    super(webClientFactory, context);
  }

  @Override
  protected void invoke(WebClient webClient, InvocationContext context) throws IOException {
    try (PrintStream out = context.getResponseStream()) {
      for (String message : WlsRestExchanges.getExchanges())
        out.println(message);
    }
  }
}
