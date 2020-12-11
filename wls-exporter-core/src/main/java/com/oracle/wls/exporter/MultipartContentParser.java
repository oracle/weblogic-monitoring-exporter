// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

class MultipartContentParser implements ParserActions {

  private final String boundary;
  private ParserState state = ParserState.INITIAL;
  private MultipartItem currentItem;
  private List<MultipartItem> items = new ArrayList<>();

  MultipartContentParser(String contentType) throws ServletException {
    Header type = new Header("Content-Type: " + contentType);
    if (!type.getValue().toLowerCase().equals("multipart/form-data")) throw new ServletException("Not multipart");
    this.boundary = type.getValue("boundary");
  }

  static List<MultipartItem> parse(HttpServletRequest request) throws ServletException {
    try {
      final MultipartContentParser parser = new MultipartContentParser(request.getContentType());
      new BufferedReader(new InputStreamReader(request.getInputStream())).lines().forEach(parser::process);
      return parser.getItems();
    } catch (IOException e) {
      throw new ServletException("Unable to parse request", e);
    }
  }

  String getBoundary() {
    return boundary;
  }

  void process(String line) {
    state = state.processLine(line, this);
  }

  MultipartItem getCurrentItem() {
    if (currentItem == null) currentItem = new MultipartItemImpl();
    return currentItem;
  }

  List<MultipartItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public ParserState getState() {
    return state;
  }

  @Override
  public boolean isStart(String line) {
    return line.equals("--" + boundary);
  }

  @Override
  public boolean isEnd(String line) {
    return line.equals("--" + boundary + "--");
  }

  @Override
  public ParserState closeItem(ParserState state) {
    items.add(currentItem);
    currentItem = null;
    return state;
  }

  @Override
  public ParserState addHeader(String line) {
    Header header = new Header(line);
    if (header.getName().equalsIgnoreCase("content-disposition")) {
      ((MultipartItemImpl) getCurrentItem()).setDisposition(header);
    } else if (header.getName().equalsIgnoreCase("content-type")) {
      ((MultipartItemImpl) getCurrentItem()).setContentType(header);
    }
    return ParserState.HEADERS;
  }

  @Override
  public ParserState addDataLine(String line) {
    ((MultipartItemImpl) getCurrentItem()).addDataLine(line);
    return ParserState.CONTENT;
  }

  static class MultipartItemImpl implements MultipartItem {
    private String name;
    private String fileName;
    private StringBuilder contents;
    private String contentType;

    @Override
    public boolean isFormField() {
      return fileName == null;
    }

    @Override
    public String getFieldName() {
      return name;
    }

    @Override
    public String getString() {
      return Optional.ofNullable(contents).map(StringBuilder::toString).orElse("");
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(getString().getBytes());
    }

    public void setDisposition(Header header) {
      name = header.getValue("name");
      fileName = header.getValue("filename");
    }

    public void setContentType(Header header) {
      contentType = header.getValue();
    }

    public void addDataLine(String line) {
      if (contents == null) {
        contents = new StringBuilder(line);
      } else {
        contents.append(System.lineSeparator()).append(line);
      }
    }
  }
}
