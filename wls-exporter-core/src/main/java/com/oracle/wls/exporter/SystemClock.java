// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.time.OffsetDateTime;

/**
 * A wrapper for the system clock that facilitates unit testing of time.
 */
public abstract class SystemClock {

  // Leave as non-final; unit tests may replace this value
  @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
  private static SystemClock delegate = new SystemClock() {
    @Override
    public OffsetDateTime getCurrentTime() {
      return OffsetDateTime.now();
    }
  };

  /**
   * Returns the current time.
   *
   * @return a time instance
   */
  public static OffsetDateTime now() {
    return delegate.getCurrentTime();
  }

  /**
   * Returns the delegate's current time.
   *
   * @return a time instance
   */
  public abstract OffsetDateTime getCurrentTime();
}
