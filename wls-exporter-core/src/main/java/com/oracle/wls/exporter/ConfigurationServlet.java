// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.wls.exporter.domain.ConfigurationException;
import com.oracle.wls.exporter.domain.ExporterConfig;

import static com.oracle.wls.exporter.ServletConstants.APPEND_ACTION;
import static com.oracle.wls.exporter.ServletConstants.DEFAULT_ACTION;
import static com.oracle.wls.exporter.ServletConstants.MAIN_PAGE;
import static com.oracle.wls.exporter.ServletConstants.REPLACE_ACTION;

/**
 * A servlet which handles updates to the exporter configuration.
 *
 * @author Russell Gold
 */
@WebServlet(value = "/" + ServletConstants.CONFIGURATION_PAGE)
public class ConfigurationServlet extends PassThroughAuthenticationServlet {

    @SuppressWarnings("unused")  // production constructor
    public ConfigurationServlet() {
        this(new WebClientFactoryImpl());
    }

    ConfigurationServlet(WebClientFactory webClientFactory) {
        super(webClientFactory);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWithAuthentication(request, response, this::updateConfiguration);
    }

    private void updateConfiguration(WebClient webClient, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            authenticate(webClient.withUrl(getAuthenticationUrl()));
            if (!isMultipartContent(req)) throw new ServletException("Must be a multi-part request");

            createPostAction(webClient, req).perform();
            reportUpdatedConfiguration(resp);
        } catch (RestPortConnectionException e) {
            reportFailure(e);
            webClient.setRetryNeeded();
        } catch (ConfigurationException e) {
            reportUnableToUpdateConfiguration(req, resp.getOutputStream(), e);
        }
    }

    // Authenticates by attempting to send a request to the Management RESTful API.
    private void authenticate(WebClient webClient) throws IOException {
        webClient.doPostRequest("{ 'links':[], 'fields':[], 'children':{} }".replace("'", "\""));
    }

    private boolean isMultipartContent(HttpServletRequest request) {
        return request.getContentType().toLowerCase(Locale.ENGLISH).startsWith("multipart/");
    }

    private void reportUpdatedConfiguration(HttpServletResponse response) throws IOException {
        response.sendRedirect(MAIN_PAGE);
    }

    private void reportUnableToUpdateConfiguration(HttpServletRequest request, ServletOutputStream out, ConfigurationException e) throws IOException {
        out.println(ServletConstants.PAGE_HEADER);
        out.println("<H1>Unable to Update Configuration</H1><p>");
        out.println(e.getMessage());
        out.println("</p>" +"</body></html>");
        out.println("<form action=\"" + request.getContextPath() + "/\">");
        out.println("    <br><input type=\"submit\" value=\"OK\">");
        out.println("</form>");
        out.close();
    }

    private PostAction createPostAction(WebClient webClient, HttpServletRequest request) throws IOException, ServletException {
        PostAction postAction = new PostAction();
        configure(postAction, MultipartContentParser.parse(request));
        return postAction;
    }

    private void configure(PostAction postAction, List<MultipartItem> fileItems) throws IOException, ServletException {
        for (MultipartItem item : fileItems) {
            if (!item.isFormField()) {
                postAction.defineUploadedFile(item.getInputStream());
            } else if (item.getFieldName().equals(ServletConstants.EFFECT_OPTION))
                postAction.setEffect(item.getString());
        }
    }

    private static class PostAction {
        // The action to take. May be either "replace" or "append"
        private String effect = DEFAULT_ACTION;
        private ExporterConfig uploadedConfig;

        private void defineUploadedFile(InputStream inputStream) throws ServletException {
            try {
                uploadedConfig = ExporterConfig.loadConfig(inputStream);
            } catch(ConfigurationException e) {
                throw e;
            } catch (Throwable e) {
                throw new ServletException("Unable to understand specified configuration");
            }
        }

        void perform() throws ServletException {
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
