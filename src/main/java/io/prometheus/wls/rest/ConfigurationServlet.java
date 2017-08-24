package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Russell Gold
 */
@WebServlet(value = "/configure")
public class ConfigurationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ServletFileUpload.isMultipartContent(request)) throw new ServletException("Must be a multi-part request");

        createPostAction(request).perform();
        reportUpdatedConfiguration(response);
    }

    private void reportUpdatedConfiguration(HttpServletResponse response) throws IOException {
        response.sendRedirect("");
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
            } else if (item.getFieldName().equals("effect"))
                postAction.setEffect(item.getString());
        }
    }

    private class PostAction {
        private String effect = "replace";
        private ExporterConfig uploadedConfig;

        private void defineUploadedFile(InputStream inputStream) throws ServletException {
            try {
                uploadedConfig = ExporterConfig.loadConfig(inputStream);
            } catch(ConfigurationException e) {
                throw new ServletException(e.getMessage());
            } catch (Throwable e) {
                throw new ServletException("Unable to understand specified configuration");
            }
        }

        void perform() throws ServletException {
            ExporterConfig uploadedConfig = this.uploadedConfig;

            if (effect.equalsIgnoreCase("replace"))
                LiveConfiguration.replaceConfiguration(uploadedConfig);
            else if (effect.equalsIgnoreCase("append"))
                LiveConfiguration.appendConfiguration(uploadedConfig);
        }

        void setEffect(String effect) {
            this.effect = effect;
        }
    }

}
