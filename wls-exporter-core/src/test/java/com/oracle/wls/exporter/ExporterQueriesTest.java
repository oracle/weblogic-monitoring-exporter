// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.oracle.wls.exporter.ExporterQueries.MAX_QUERIES;
import static com.oracle.wls.exporter.ExporterQueryFormatter.format;
import static com.oracle.wls.exporter.webapp.HttpServletRequestStub.createGetRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ExporterQueriesTest {

    static final int NUM_QUERIES = MAX_QUERIES + 5;
    private final Instant startInstant = Instant.parse("2024-05-15T18:35:24.00Z");

    private final ClockStub clock = createStrictStub(ClockStub.class);
    private final List<Memento> mementos = new ArrayList<>();

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        mementos.add(StaticStubSupport.install(ExporterQueries.class, "clock", clock));

        clock.setCurrentMsec(startInstant.toEpochMilli());
        ExporterQueries.clear();
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    void whenMultipleQueriesReceived_collectThem() {
        receiveQueries(this::sameTime, 3);

        assertThat(ExporterQueries.getQueries(), hasItems(
                format("host1", startInstant),
                format("host2", startInstant),
                format("host3", startInstant)));
    }

    private void receiveQueries(Consumer<ClockStub> timeUpdates, int numHosts) {
        for (int i = 1; i <= numHosts; i++) {
            ExporterQueries.addQuery(createRequestFrom(toHostName(String.valueOf(i))));
            timeUpdates.accept(clock);
        }
    }

    private HttpServletRequest createRequestFrom(String remoteHost) {
        return createGetRequest().withRemoteHost(remoteHost);
    }

    @Test
    void afterMultipleQueriesReceived_clearRemovesThem() {
        receiveQueries(this::sameTime, 3);
        ExporterQueries.clear();

        assertThat(ExporterQueries.getQueries(), empty());
    }

    @Test
    void whenMultipleQueriesReceivedOverTime_collectThem() {
        receiveQueries(this::addOneSecond, 3);

        assertThat(ExporterQueries.getQueries(), hasItems(
                format("host1", startInstant),
                format("host2", startInstant.plus(1, ChronoUnit.SECONDS)),
                format("host3", startInstant.plus(2, ChronoUnit.SECONDS))));
    }

    @Test
    void whenMoreThanAllowedQueriesReceived_pruneThem() {
        receiveQueries(this::addOneSecond, NUM_QUERIES);

        assertThat(ExporterQueries.getQueries(), hasSize(MAX_QUERIES));
        assertThat(ExporterQueries.getQueries(), hasItems(getPrunedMatchers(NUM_QUERIES)));
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private Matcher<String>[] getPrunedMatchers(int lastHost) {
        int firstHost = lastHost - MAX_QUERIES + 1;
        Matcher<?>[] matchers = IntStream.rangeClosed(firstHost, MAX_QUERIES)
                .mapToObj(String::valueOf)
                .map(this::toHostName)
                .map(Matchers::containsString)
                .toArray(Matcher[]::new);
        return (Matcher<String>[]) matchers;
    }

    private String toHostName(String i) {
        return "host" + i;
    }

    private void sameTime(ClockStub clockStub) {
    }

    private void addOneSecond(ClockStub clockStub) {
        clockStub.incrementSeconds(1);
    }
}


