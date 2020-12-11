// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Header {

  public static final String QUOTE = "\"";
  private final String name;
  private String value;
  private final Map<String,String> parameters = new HashMap<>();

  public Header(String headerLine) {
    String[] split = headerLine.split(":");
    name = split[0].trim();
    if (split.length > 1)
      Arrays.stream(split[1].split(";")).forEach(this::parse);
  }

  private void parse(String s) {
    if (value == null)
      value = s.trim();
    else {
      String[] split = s.split("=");
      if (split.length >= 2) {
        parameters.put(trim(split[0]), trim(split[1]));
      }
    }
  }

  private String trim(String value) {
    value = value.trim();
    if (value.startsWith(QUOTE) && value.endsWith(QUOTE))
      value = value.substring(1, value.length()-1);
    return value;
  }

  public String getValue() {
    return value;
  }

  public String getValue(String key) {
    return parameters.get(key);
  }

  public String getName() {
    return name;
  }
}
