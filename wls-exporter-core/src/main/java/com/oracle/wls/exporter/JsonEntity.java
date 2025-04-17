// Copyright (c) 2021, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;

public class JsonEntity<T> implements HttpEntity {

  static final Charset CHARSET = StandardCharsets.UTF_8;
  private final byte[] representation;
  private final Header contentEncoding;
  private final Header contentType;

  public JsonEntity(T item) {
    representation = new Gson().toJson(item).getBytes(CHARSET);
    contentEncoding = new BasicHeader(HTTP.CONTENT_ENCODING, CHARSET.toString());
    contentType = new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
  }

  @Override
  public boolean isRepeatable() {
    return true;
  }

  @Override
  public boolean isChunked() {
    return false;
  }

  @Override
  public long getContentLength() {
    return representation.length;
  }

  @Override
  public Header getContentType() {
    return contentType;
  }

  @Override
  public Header getContentEncoding() {
    return contentEncoding;
  }

  @Override
  public InputStream getContent() throws UnsupportedOperationException {
    return new ByteArrayInputStream(representation);
  }

  @Override
  public void writeTo(OutputStream outStream) throws IOException {
    Args.notNull(outStream, "Output stream");
    outStream.write(representation);
    outStream.flush();
  }

  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  @Deprecated
  public void consumeContent() {
    // no-op
  }
}
