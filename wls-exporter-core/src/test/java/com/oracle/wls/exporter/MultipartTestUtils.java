// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import static com.oracle.wls.exporter.HttpServletRequestStub.createPostRequest;

public class MultipartTestUtils {

  private final static String BOUNDARY = "C3n5NKoslNBKj4wBHR8kCX6OtVYEqeFYNjorlBP";

  static HttpServletRequestStub createUploadRequest(String contents) {
    return createPostRequest().withMultipartContent(contents, BOUNDARY);
  }

  static String createEncodedForm(String effect, String configuration) throws IOException {
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.setBoundary(BOUNDARY);
      builder.addTextBody("effect", effect);
      builder.addBinaryBody("configuration", configuration.getBytes(), ContentType.create("text/plain", Charset.defaultCharset()), "newconfig.yml");
      HttpEntity entity = builder.build();
      return asString(entity);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  static String asString(HttpEntity entity) throws IOException {
      byte[] result = new byte[(int) entity.getContentLength()];
      InputStream inputStream = entity.getContent();
      inputStream.read(result);
      return new String(result);
  }
}
