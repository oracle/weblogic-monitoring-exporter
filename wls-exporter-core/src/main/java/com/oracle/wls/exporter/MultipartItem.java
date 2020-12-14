// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.InputStream;

/**
 * An item in a multi-part http message. Will be either a form field or file data.
 *
 * @author Russell Gold
 */
interface MultipartItem {

  /**
   * Returns true if this item represents a form field.
   */
  boolean isFormField();

  /**
   * Returns the name of this field.
   */
  String getFieldName();

  /**
   * Returns the value of this item as a string.
   */
  String getString();

  /**
   * Returns the value of this item as an input stream.
   */
  InputStream getInputStream() throws IOException;
}
