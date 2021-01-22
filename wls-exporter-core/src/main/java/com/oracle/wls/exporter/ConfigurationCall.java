// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;

import com.oracle.wls.exporter.domain.ConfigurationException;
import com.oracle.wls.exporter.domain.ExporterConfig;

public abstract class ConfigurationCall extends AuthenticatedCall {

  public ConfigurationCall(WebClientFactory webClientFactory, InvocationContext context) {
    super(webClientFactory, context);
  }

  @Override
  protected void invoke(WebClient webClient, InvocationContext context) throws IOException {
    try (InputStream requestStream = context.getRequestStream()){
      authenticate(webClient.withUrl(getAuthenticationUrl()));
      createConfigurationAction(getRequestContentType(), requestStream).perform();
      reportUpdatedConfiguration(context);
    } catch (RestPortConnectionException e) {
      reportFailure(e);
      webClient.setRetryNeeded();
    } catch (ConfigurationException e) {
      reportUnableToUpdateConfiguration(context, e);
    }
  }

  // Authenticates by attempting to send a request to the Management RESTful API.
  private void authenticate(WebClient webClient) throws IOException {
    webClient.doPostRequest("{ 'links':[], 'fields':[], 'children':{} }".replace("'", "\""));
  }

  abstract ConfigurationAction createConfigurationAction(String contentType, InputStream inputStream) throws IOException;

  protected abstract void reportUpdatedConfiguration(InvocationContext context) throws IOException;

  abstract void reportUnableToUpdateConfiguration(InvocationContext context, ConfigurationException e) throws IOException;

  static abstract class ConfigurationAction {

    private ExporterConfig uploadedConfig;

    final void defineUploadedFile(InputStream inputStream) {
      try {
        uploadedConfig = ExporterConfig.loadConfig(inputStream);
      } catch (ConfigurationException e) {
        throw e;
      } catch (Throwable e) {
        throw new RuntimeException("Unable to understand specified configuration");
      }
    }

    public abstract void perform();

    ExporterConfig getUploadedConfig() {
      return uploadedConfig;
    }
  }
}
