// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.oracle.wls.exporter.ExporterQueries.MAX_QUERIES;
import static com.oracle.wls.exporter.ExporterQueryFormatter.format;
import static com.oracle.wls.exporter.webapp.HttpServletRequestStub.createGetRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ExporterQueriesTest {

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
        receiveQueries(this::sameTime, "host1", "host2", "host3");

        assertThat(ExporterQueries.getQueries(), hasItems(
                format("host1", startInstant),
                format("host2", startInstant),
                format("host3", startInstant)));
    }

    private void receiveQueries(Consumer<ClockStub> timeUpdates, String... remoteHosts) {
        for (String remoteHost : remoteHosts) {
            ExporterQueries.addQuery(createRequestFrom(remoteHost));
            timeUpdates.accept(clock);
        }
    }

    private HttpServletRequest createRequestFrom(String remoteHost) {
        return createGetRequest().withRemoteHost(remoteHost);
    }

    @Test
    void afterMultipleQueriesReceived_clearRemovesThem() {
        receiveQueries(this::sameTime, "host1", "host2", "host3");
        ExporterQueries.clear();

        assertThat(ExporterQueries.getQueries(), empty());
    }

    @Test
    void whenMultipleQueriesReceivedOverTime_collectThem() {
        receiveQueries(this::addOneSecond, "host1", "host2", "host3");

        assertThat(ExporterQueries.getQueries(), hasItems(
                format("host1", startInstant),
                format("host2", startInstant.plus(1, ChronoUnit.SECONDS)),
                format("host3", startInstant.plus(2, ChronoUnit.SECONDS))));
    }

    @Test
    void whenMoreThanAllowedQueriesReceived_pruneThem() {
        receiveQueries(this::addOneSecond,
                "host1", "host2", "host3", "host4", "host5", "host6", "host7", "host8");

        assertThat(ExporterQueries.getQueries(), hasSize(MAX_QUERIES));
        assertThat(ExporterQueries.getQueries(), hasItems(
                containsString("host3"),
                containsString("host4"),
                containsString("host5"),
                containsString("host6"),
                containsString("host7"),
                containsString("host8")));
    }

    private void sameTime(ClockStub clockStub) {
    }

    private void addOneSecond(ClockStub clockStub) {
        clockStub.incrementSeconds(1);
    }
}


