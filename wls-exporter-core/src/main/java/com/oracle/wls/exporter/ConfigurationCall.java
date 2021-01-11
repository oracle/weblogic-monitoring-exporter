// Copyright (c) 2021, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import com.oracle.wls.exporter.domain.ConfigurationException;
import com.oracle.wls.exporter.domain.ExporterConfig;

import static com.oracle.wls.exporter.WebAppConstants.APPEND_ACTION;
import static com.oracle.wls.exporter.WebAppConstants.DEFAULT_ACTION;
import static com.oracle.wls.exporter.WebAppConstants.MAIN_PAGE;
import static com.oracle.wls.exporter.WebAppConstants.REPLACE_ACTION;

public class ConfigurationCall extends AuthenticatedCall {

  public ConfigurationCall(WebClientFactory webClientFactory, InvocationContext context) {
    super(webClientFactory, context);
  }

  @Override
  protected void invoke(WebClient webClient, InvocationContext context) throws IOException {
    try {
      if (!isMultipartContent()) throw new RuntimeException("Must be a multi-part request");

      authenticate(webClient.withUrl(getAuthenticationUrl()));
      createPostAction(getRequestContentType(), getRequestStream()).perform();
      reportUpdatedConfiguration(context);
    } catch (RestPortConnectionException e) {
      reportFailure(e);
      webClient.setRetryNeeded();
    } catch (ConfigurationException e) {
      reportUnableToUpdateConfiguration(context.getApplicationContext(), context.getResponseStream(), e);
    }
  }

  private boolean isMultipartContent() {
    return getRequestContentType().toLowerCase(Locale.ENGLISH).startsWith("multipart/");
  }

  // Authenticates by attempting to send a request to the Management RESTful API.
  private void authenticate(WebClient webClient) throws IOException {
    webClient.doPostRequest("{ 'links':[], 'fields':[], 'children':{} }".replace("'", "\""));
  }

  private PostAction createPostAction(String contentType, InputStream inputStream) throws IOException {
    PostAction postAction = new PostAction();
    configure(postAction, MultipartContentParser.parse(contentType, inputStream));
    return postAction;
  }

  private void configure(PostAction postAction, List<MultipartItem> fileItems) throws IOException {
    for (MultipartItem item : fileItems) {
      if (!item.isFormField()) {
        postAction.defineUploadedFile(item.getInputStream());
      } else if (item.getFieldName().equals(WebAppConstants.EFFECT_OPTION))
        postAction.setEffect(item.getString());
    }
  }

  private void reportUpdatedConfiguration(InvocationContext context) throws IOException {
    context.sendRedirect(MAIN_PAGE);
  }

  private void reportUnableToUpdateConfiguration(String contextPath, PrintStream out, ConfigurationException e) throws IOException {
    out.println(WebAppConstants.PAGE_HEADER);
    out.println("<H1>Unable to Update Configuration</H1><p>");
    out.println(e.getMessage());
    out.println("</p>" + "</body></html>");
    out.println("<form action=\"" + contextPath + "/\">");
    out.println("    <br><input type=\"submit\" value=\"OK\">");
    out.println("</form>");
    out.close();
  }

  private static class PostAction {

    // The action to take. May be either "replace" or "append"
    private String effect = DEFAULT_ACTION;
    private ExporterConfig uploadedConfig;

    private void defineUploadedFile(InputStream inputStream) {
      try {
        uploadedConfig = ExporterConfig.loadConfig(inputStream);
      } catch (ConfigurationException e) {
        throw e;
      } catch (Throwable e) {
        throw new RuntimeException("Unable to understand specified configuration");
      }
    }

    void perform() {
      ExporterConfig uploadedConfig = this.uploadedConfig;

      if (effect.equalsIgnoreCase(REPLACE_ACTION))
        LiveConfiguration.replaceConfiguration(uploadedConfig);
      else if (effect.equalsIgnoreCase(APPEND_ACTION))
        LiveConfiguration.appendConfiguration(uploadedConfig);
    }

    void setEffect(String effect) {
      this.effect = effect;
    }
  }
}
