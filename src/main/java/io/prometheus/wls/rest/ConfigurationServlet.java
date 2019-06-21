package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import io.prometheus.wls.rest.domain.ConfigurationException;
import io.prometheus.wls.rest.domain.ExporterConfig;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static io.prometheus.wls.rest.ServletConstants.*;

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
        authenticate(webClient.withUrl(LiveConfiguration.getAuthenticationUrl()));
        try {
            if (!ServletFileUpload.isMultipartContent(req)) throw new ServletException("Must be a multi-part request");

            createPostAction(req).perform();
            reportUpdatedConfiguration(resp);
        } catch (ConfigurationException e) {
            reportUnableToUpdateConfiguration(req, resp.getOutputStream(), e);
        }
    }

    // Authenticates by attempting to send a request to the Management RESTful API.
    private void authenticate(WebClient webClient) throws IOException {
        webClient.doGetRequest();
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

    private PostAction createPostAction(HttpServletRequest request) throws IOException, ServletException {
        PostAction postAction = new PostAction();
        try {
            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
            configure(postAction, upload.parseRequest(request));
        } catch (FileUploadException e) {
            throw new ServletException("unable to parse post body", e);
        }
        return postAction;
    }

    private void configure(PostAction postAction, List<FileItem> fileItems) throws IOException, ServletException {
        for (FileItem item : fileItems) {
            if (!item.isFormField()) {
                postAction.defineUploadedFile(item.getInputStream());
            } else if (item.getFieldName().equals(ServletConstants.EFFECT_OPTION))
                postAction.setEffect(item.getString());
        }
    }

    private class PostAction {
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
