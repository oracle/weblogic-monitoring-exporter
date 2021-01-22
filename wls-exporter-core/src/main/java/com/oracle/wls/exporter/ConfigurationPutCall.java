// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.oracle.wls.exporter.domain.ConfigurationException;

public class ConfigurationPutCall extends ConfigurationCall {

  private static final Set<String> SUPPORTED_CONTENT_TYPES
        = new HashSet<>(Arrays.asList("application/yaml", "text/yaml"));

  public ConfigurationPutCall(WebClientFactory webClientFactory, InvocationContext context) {
    super(webClientFactory, context);
  }

  @Override
  ConfigurationAction createConfigurationAction(String contentType, InputStream inputStream) {
    return new ConfigurationPutAction(contentType, inputStream);
  }

  @Override
  protected void reportUpdatedConfiguration(InvocationContext context) {
    // do nothing
  }

  @Override
  void reportUnableToUpdateConfiguration(InvocationContext context, ConfigurationException e) throws IOException {
    context.sendError(HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage());
  }

  static class ConfigurationPutAction extends ConfigurationAction {

    public ConfigurationPutAction(String contentType, InputStream inputStream) {
      if (!isSupportedContentType(contentType))
        throw new ServerErrorException(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported content type: " + contentType);

      defineUploadedFile(inputStream);
    }

    private boolean isSupportedContentType(String contentType) {
      return SUPPORTED_CONTENT_TYPES.contains(contentType);
    }

    @Override
    public void perform() {
      LiveConfiguration.replaceConfiguration(getUploadedConfig());
    }
  }
}
