// Copyright (c) 2020, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import com.oracle.wls.exporter.javax.HttpServletRequestStub;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipartContentParserTest {

  private static final String BOUNDARY = "---------------------------9051914041544843365972754266";
  private MultipartContentParser parser;

  @BeforeEach
  public void setUp() {
    parser = new MultipartContentParser("multipart/form-data; boundary=" + BOUNDARY);
  }

  @Test
  void whenContentTypeIsNotMultiForm_throwException() {
    assertThrows(RuntimeException.class, () -> new MultipartContentParser("text/plain"));
  }

  @Test
  void whenCreated_boundaryIsDefined() {
    assertThat(parser.getBoundary(), equalTo(BOUNDARY));
  }

  @Test
  void parseInitialState() {
    assertThat(parser.getState(), sameInstance(ParserState.INITIAL));
  }

  @Test
  void whenInitialStateAndReceivedBoundary_watchForHeaders() {
    parser.process("--" + BOUNDARY);

    assertThat(parser.getState(), sameInstance(ParserState.HEADERS));
  }

  @Test
  void WhenReceiveFieldContentDispositionHeaderInHeaderState_defineField() {
    parser.process("--" + BOUNDARY);

    parser.process("Content-Disposition: form-data; name=\"text\"");

    assertThat(parser.getCurrentItem().isFormField(), is(true));
    assertThat(parser.getCurrentItem().getFieldName(), equalTo("text"));
  }

  @Test
  void WhenReceiveBlankLineInHeaderState_watchForContent() {
    parser.process("--" + BOUNDARY);
    parser.process("Content-Disposition: form-data; name=\"text\"");

    parser.process("");

    assertThat(parser.getState(), sameInstance(ParserState.CONTENT));
  }

  @Test
  void WhenNoContentReceived_itemHasEmptyData() {
    assertThat(parser.getCurrentItem().getString(), equalTo(""));
  }

  @Test
  void WhenReceiveDataInContentState_addToDefinition() {
    parser.process("--" + BOUNDARY);
    parser.process("Content-Disposition: form-data; name=\"text\"");
    parser.process("");
    parser.process("value");

    assertThat(parser.getCurrentItem().getString(), equalTo("value"));
  }

  @Test
  void WhenReceiveNewBoundaryInContentState_defineItem() {
    parser.process("--" + BOUNDARY);
    parser.process("Content-Disposition: form-data; name=\"text\"");
    parser.process("");
    parser.process("value");
    parser.process("--" + BOUNDARY);

    assertThat(parser.getItems(), hasSize(1));
  }

  @Test
  void parseFormFields() throws IOException {
    BufferedReader sample = new BufferedReader(new StringReader(SAMPLE));
    sample.lines().forEach(parser::process);

    assertThat(getItem("text").isFormField(), is(true));
    assertThat(getItem("text").getString(), equalTo("text default"));

    assertThat(getItem("file1").isFormField(), is(false));
    assertThat(readInputStream(getItem("file1").getInputStream()), equalTo("Line 1\nLine 2"));

    assertThat(getItem("file2").isFormField(), is(false));
    assertThat(readInputStream(getItem("file2").getInputStream()), equalTo("<!DOCTYPE html><title>Content of a.html.</title>"));
  }

  private MultipartItem getItem(String name) {
    return parser.getItems().stream().filter(i -> i.getFieldName().equals(name)).findFirst().orElse(null);
  }

  private String readInputStream(InputStream is) {
    return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining(System.lineSeparator()));
  }

  private static final String SAMPLE =
              "--" + BOUNDARY + "\n" +
              "Content-Disposition: form-data; name=\"text\"\n" +
              "\n" +
              "text default\n" +
              "--" + BOUNDARY + "\n" +
              "Content-Disposition: form-data; name=\"file1\"; filename=\"a.txt\"\n" +
              "Content-Type: text/plain\n" +
              "\n" +
              "Line 1\n" +
              "Line 2\n" +
              "\n" +
              "--" + BOUNDARY +"\n" +
              "Content-Disposition: form-data; name=\"file2\"; filename=\"a.html\"\n" +
              "Content-Type: text/html\n" +
              "\n" +
              "<!DOCTYPE html><title>Content of a.html.</title>\n" +
              "\n" +
              "--" + BOUNDARY + "--";




  @Test
  void whenMultipartRequestNotParseable_throwException() {
      HttpServletRequest request = HttpServletRequestStub.createPostRequest();

      assertThrows(RuntimeException.class,
            () -> MultipartContentParser.parse(request.getContentType(), new ByteArrayInputStream(new byte[0])));
  }

  @Test
  void whenMultipartRequestContainsFormFields_allAreMarkedAsFormFields() throws IOException {
      HttpEntity httpEntity = MultipartEntityBuilder.create()
            .setBoundary(BOUNDARY)
            .addTextBody("field1", "value1")
            .addTextBody("field2", "value2")
            .build();

      assertThat(toRequestStream(httpEntity).allMatch(MultipartItem::isFormField), is(true));
  }

  protected Stream<MultipartItem> toRequestStream(HttpEntity httpEntity) throws IOException {
      HttpServletRequest request = toPostRequest(httpEntity);
      return MultipartContentParser.parse(request.getContentType(), request.getInputStream()).stream();
  }

  protected static HttpServletRequest toPostRequest(HttpEntity httpEntity) throws IOException {
      return HttpServletRequestStub.createPostRequest()
            .withMultipartContent(MultipartTestUtils.asString(httpEntity), BOUNDARY);
  }

  @Test
  void whenMultipartRequestContainsFormFields_retrieveThem() throws IOException {
      HttpEntity httpEntity = MultipartEntityBuilder.create()
            .setBoundary(BOUNDARY)
            .addTextBody("field1", "value1")
            .addTextBody("field2", "value2")
            .build();

      final Map<String, String> entries = toRequestStream(httpEntity)
            .collect(Collectors.toMap(MultipartItem::getFieldName, MultipartItem::getString));

      assertThat(entries, Matchers.allOf(hasEntry("field1", "value1"), hasEntry("field2", "value2")));
  }

  @Test
  void whenMultipartRequestContainsBinaryEntries_nonAreMarkedAsFormFields() throws IOException {
      HttpEntity httpEntity = MultipartEntityBuilder.create()
            .setBoundary(BOUNDARY)
            .addBinaryBody("file1", "value1".getBytes(), ContentType.DEFAULT_BINARY, "/path/to/file1.txt")
            .addBinaryBody("file2", "value2".getBytes(), ContentType.DEFAULT_BINARY, "/path/to/file2.txt")
            .build();

      assertThat(toRequestStream(httpEntity).noneMatch(MultipartItem::isFormField), is(true));
  }

  @Test
  void whenMultipartRequestContainsBinaryEntries_retrieveThem() throws IOException {
      HttpEntity httpEntity = MultipartEntityBuilder.create()
            .setBoundary(BOUNDARY)
            .addBinaryBody("file1", "value1".getBytes(UTF_8), ContentType.DEFAULT_BINARY, "/path/to/file1.txt")
            .addBinaryBody("file2", "value2".getBytes(UTF_8), ContentType.DEFAULT_BINARY, "/path/to/file2.txt")
            .build();

      final Map<String, String> entries = toRequestStream(httpEntity)
            .collect(Collectors.toMap(MultipartItem::getFieldName, this::getInputStreamAsString));


      assertThat(entries, Matchers.allOf(hasEntry("file1", "value1"), hasEntry("file2", "value2")));
  }

  private String getInputStreamAsString(MultipartItem item) {
      try {
          return new BufferedReader(
            new InputStreamReader(item.getInputStream(), StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }
}
