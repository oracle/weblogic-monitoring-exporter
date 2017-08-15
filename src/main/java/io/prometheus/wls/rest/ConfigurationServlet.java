package io.prometheus.wls.rest;

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

    private void configure(PostAction postAction, List<FileItem> fileItems) throws IOException {
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

        private void defineUploadedFile(InputStream inputStream) {
            uploadedConfig = ExporterConfig.loadConfig(inputStream);
        }

        void perform() throws ServletException {
            if (LiveConfiguration.getConfig() == null)
                throw new ServletException("Exporter Servlet not initialized");
            if (uploadedConfig == null)
                throw new ServletException("No configuration specified");

            if (effect.equalsIgnoreCase("replace"))
                LiveConfiguration.getConfig().replace(uploadedConfig);
            else if (effect.equalsIgnoreCase("append"))
                LiveConfiguration.getConfig().append(uploadedConfig);
        }

        void setEffect(String effect) {
            this.effect = effect;
        }
    }
}
