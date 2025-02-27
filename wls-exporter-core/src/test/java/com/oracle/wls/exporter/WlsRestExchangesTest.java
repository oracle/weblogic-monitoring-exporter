// Copyright (c) 2021, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import com.meterware.simplestub.Memento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.oracle.wls.exporter.WlsRestExchanges.MAX_EXCHANGES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

class WlsRestExchangesTest {

  private static final String URL = "http://localhost:7001";
  private static final int EXCESS_EXCHANGES = 3;
  private static final long FOUR_MINUTES_IN_SECONDS = 4 * 60;

  private final List<Memento> mementos = new ArrayList<>();

  @BeforeEach
  public void setUp() throws NoSuchFieldException {
    mementos.add(SystemClockTestSupport.installClock());
    WlsRestExchanges.clear();
  }

  @AfterEach
  void tearDown() {
    mementos.forEach(Memento::revert);
  }

  @Test
  void currentExchangesList_includesRecentExchanges() {
    IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addTestExchange);

    String exchanges = getExchanges();

    assertThat(exchanges, containsString("request 4"));
  }

  private void addTestExchange(int i) {
    WlsRestExchanges.addExchange(URL, "request " + i, "response " + i);
  }

  private static String getExchanges() {
    return WlsRestExchanges.getExchanges().stream().collect(Collectors.joining(System.lineSeparator()));
  }

  @Test
  void whenAdditionalExchangesAdd_removeOlderExchanges() {
    IntStream.rangeClosed(1, MAX_EXCHANGES + EXCESS_EXCHANGES).forEach(this::addTestExchange);

    String exchanges = getExchanges();

    assertThat(exchanges, not(containsString("request 1")));
  }

  @Test
  void whenEarlyExchangesAdd_totalMessageAllocationIncreases() {
    addTestExchange(1);
    final int initialSize = WlsRestExchanges.getMessageAllocation();

    addTestExchange(2);

    assertThat(WlsRestExchanges.getMessageAllocation(), greaterThan(initialSize));
  }

  @Test
  void whenLongExchangesReplacedByShorterOnes_totalMessageAllocationDecreases() {
    IntStream.rangeClosed(1, MAX_EXCHANGES).forEach(this::addLongTestExchange);
    final int initialSize = WlsRestExchanges.getMessageAllocation();

    addTestExchange(MAX_EXCHANGES+1);

    assertThat(WlsRestExchanges.getMessageAllocation(), lessThan(initialSize));
  }

  private void addLongTestExchange(int i) {
    WlsRestExchanges.addExchange(URL, "a longer request " + i, "a longer response " + i);
  }

  @Test
  void getMaximumExchangeLength() {
    addVariableLengthExchange(0);
    final int minimumExchangeLength = WlsRestExchanges.getMessageAllocation();

    addVariableLengthExchange(10);
    addVariableLengthExchange(20);
    addVariableLengthExchange(15);

    assertThat(WlsRestExchanges.getMaximumExchangeLength(), equalTo(20 + minimumExchangeLength));
  }

  private void addVariableLengthExchange(int responseLength) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < responseLength; i++) sb.append(" ");
    WlsRestExchanges.addExchange(URL, "", sb.toString());
  }

  @Test
  void getTotalExchangeLengthOverPastTenMinutes() {
    addVariableLengthExchange(0);
    final int minimumExchangeLength = WlsRestExchanges.getMessageAllocation();

    addVariableLengthExchange(10);

    SystemClockTestSupport.increment(FOUR_MINUTES_IN_SECONDS);
    addVariableLengthExchange(20);

    SystemClockTestSupport.increment(FOUR_MINUTES_IN_SECONDS);
    addVariableLengthExchange(15);

    SystemClockTestSupport.increment(FOUR_MINUTES_IN_SECONDS);
    addVariableLengthExchange(40);

    final int expectedLength = 3 * minimumExchangeLength + 20 + 15 + 40;
    assertThat(WlsRestExchanges.getTotalExchangeLengthOverPastTenMinutes(), equalTo(expectedLength));
  }
}