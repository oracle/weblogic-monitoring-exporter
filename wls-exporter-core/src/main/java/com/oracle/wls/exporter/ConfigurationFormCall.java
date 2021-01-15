// Copyright (c) 2021, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import com.oracle.wls.exporter.domain.ConfigurationException;

import static com.oracle.wls.exporter.WebAppConstants.APPEND_ACTION;
import static com.oracle.wls.exporter.WebAppConstants.DEFAULT_ACTION;
import static com.oracle.wls.exporter.WebAppConstants.MAIN_PAGE;
import static com.oracle.wls.exporter.WebAppConstants.REPLACE_ACTION;

public class ConfigurationFormCall extends ConfigurationCall {

  public ConfigurationFormCall(WebClientFactory webClientFactory, InvocationContext context) {
    super(webClientFactory, context);
  }

  @Override
  ConfigurationAction createConfigurationAction(String contentType, InputStream inputStream) throws IOException {
    return new ConfigurationFormAction(contentType, inputStream);
  }

  @Override
  protected void reportUpdatedConfiguration(InvocationContext context) throws IOException {
    context.sendRedirect(MAIN_PAGE);
  }

  @Override
  void reportUnableToUpdateConfiguration(InvocationContext context, ConfigurationException e) throws IOException {
    PrintStream out = context.getResponseStream();
    out.println(WebAppConstants.PAGE_HEADER);
    out.println("<H1>Unable to Update Configuration</H1><p>");
    out.println(e.getMessage());
    out.println("</p>" + "</body></html>");
    out.println("<form action=\"" + context.getApplicationContext() + "/\">");
    out.println("    <br><input type=\"submit\" value=\"OK\">");
    out.println("</form>");
    out.close();
  }

  private static class ConfigurationFormAction extends ConfigurationAction {

    // The action to take. May be either "replace" or "append"
    private String effect = DEFAULT_ACTION;

    public ConfigurationFormAction(String contentType, InputStream inputStream) throws IOException {
      if (!isMultipartContent(contentType)) throw new RuntimeException("Must be a multi-part request");

      configure(MultipartContentParser.parse(contentType, inputStream));
    }

    private boolean isMultipartContent(String contentType) {
      return contentType.toLowerCase(Locale.ENGLISH).startsWith("multipart/");
    }

    private void configure(List<MultipartItem> fileItems) throws IOException {
      for (MultipartItem item : fileItems) {
        if (!item.isFormField()) {
          defineUploadedFile(item.getInputStream());
        } else if (item.getFieldName().equals(WebAppConstants.EFFECT_OPTION))
          setEffect(item.getString());
      }
    }

    void setEffect(String effect) {
      this.effect = effect;
    }

    @Override
    public void perform() {
      if (effect.equalsIgnoreCase(REPLACE_ACTION))
        LiveConfiguration.replaceConfiguration(getUploadedConfig());
      else if (effect.equalsIgnoreCase(APPEND_ACTION))
        LiveConfiguration.appendConfiguration(getUploadedConfig());
    }
  }
}
