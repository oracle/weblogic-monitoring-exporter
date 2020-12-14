// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class HeaderTest {

  @Test
  public void fetchHeaderName() {
    Header header = new Header("Content-type: text/plain");

    assertThat(header.getName(), equalTo("Content-type"));
  }

  @Test
  public void fetchMainValue() {
    Header header = new Header("Content-type: text/plain");

    assertThat(header.getValue(), equalTo("text/plain"));
  }

  @Test
  public void whenSeparatorPresent_truncateValue() {
    Header header = new Header("Content-type: text/plain; more stuff");

    assertThat(header.getValue(), equalTo("text/plain"));
  }

  @Test
  public void whenParametersPresent_fetchValues() {
    Header header = new Header("Content-disposition: form-data; name=\"file1\"; filename=\"a.txt\"");

    assertThat(header.getValue(), equalTo("form-data"));
    assertThat(header.getValue("name"), equalTo("file1"));
    assertThat(header.getValue("filename"), equalTo("a.txt"));
  }
}
