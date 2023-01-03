// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;

public class SystemClockTestSupport {
  private static TestSystemClock clock;

  public static Memento installClock() throws NoSuchFieldException {
    clock = new TestSystemClock();
    return StaticStubSupport.install(SystemClock.class, "delegate", clock);
  }

  /**
   * Increments the system clock by the specified number of seconds.
   * @param numSeconds the number of seconds by which to advance the system clock
   */
  public static void increment(long numSeconds) {
    clock.increment(numSeconds);
  }

  static class TestSystemClock extends SystemClock {
    private final OffsetDateTime testStartTime = SystemClock.now().truncatedTo(ChronoUnit.SECONDS);
    private OffsetDateTime currentTime = testStartTime;

    @Override
    public OffsetDateTime getCurrentTime() {
      return currentTime;
    }

    void increment(long numSeconds) {
      currentTime = currentTime.plusSeconds(numSeconds);
    }
  }

}
