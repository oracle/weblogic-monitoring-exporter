// Copyright (c) 2022, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.time.Clock;
import java.time.Instant;

/**
 * A unit-test implementation of the Clock interface.
 */
public abstract class ClockStub extends Clock {
  private long currentMsec = 1000L;

  public void setCurrentMsec(long currentMsec) {
    this.currentMsec = currentMsec;
  }

  public void incrementSeconds(long seconds) {
    this.currentMsec += 1000 * seconds;
  }

  @Override
  public Instant instant() {
    return Instant.ofEpochMilli(currentMsec);
  }
}
