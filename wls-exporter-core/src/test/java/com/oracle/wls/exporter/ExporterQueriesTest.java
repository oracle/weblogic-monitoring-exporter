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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.meterware.simplestub.Stub.createStrictStub;
import static com.oracle.wls.exporter.ExporterQueries.MAX_QUERIES;
import static com.oracle.wls.exporter.ExporterQueriesTest.QueryCompletion.completion;
import static com.oracle.wls.exporter.ExporterQuery.INSTANT_FORMATTER;
import static com.oracle.wls.exporter.ExporterQuery.QUERY_TIMEOUT_SECONDS;
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
        mementos.add(StaticStubSupport.install(ExporterQuery.class, "clock", clock));

        clock.setCurrentMsec(startInstant.toEpochMilli());
        ExporterQueries.clear();
    }

    @AfterEach
    void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    void whenMultipleQueriesReceived_collectThem() {
        receiveQueries(3);

        assertThat(getQueryHosts(), hasItems("host1", "host2", "host3"));
    }

    private List<String> getQueryHosts() {
        return ExporterQueries.getQueries().stream().map(ExporterQuery::getRequest).map(ServletRequest::getRemoteHost).collect(Collectors.toList());
    }

    void receiveQueries(int numQueries, QueryCompletion... exceptions) {
        List<HttpServletRequest> requests = createQueries(numQueries);

        for (int i = 0; i < numQueries; i++) {
            HttpServletRequest request = requests.get(i);
            ExporterQueries.addQuery(request);
            clock.incrementSeconds(1);
            getExceptionFor(i, exceptions).takeAction(request, clock);

        }

    }

    private QueryCompletionAction getExceptionFor(int i, QueryCompletion[] exceptions) {
        for (QueryCompletion exception : exceptions) {
            if (exception.index == i) {
                return exception.action;
            }
        }
        return QueryCompletionAction.COMPLETE;
    }

    private List<HttpServletRequest> createQueries(int numQueries) {
        return IntStream.rangeClosed(1, numQueries).boxed().map(String::valueOf).map(this::toHostName).map(this::createRequestFrom).collect(Collectors.toList());
    }

    private enum QueryCompletionAction {
        COMPLETE {
            @Override
            void takeAction(HttpServletRequest request, ClockStub clock) {
                ExporterQueries.completeQuery(request);
                clock.incrementSeconds(1);
            }
        },
        OVERLAP {
            @Override
            void takeAction(HttpServletRequest request, ClockStub clock) {
                clock.incrementSeconds(4);
                ExporterQueries.completeQuery(request);
                clock.decrementSeconds(4);
            }
        },
        NONE;

        void takeAction(HttpServletRequest request, ClockStub clock) {}
    }

    static class QueryCompletion {
        private final int index;
        private final QueryCompletionAction action;

        private QueryCompletion(int index, QueryCompletionAction action) {
            this.index = index;
            this.action = action;
        }

        static QueryCompletion completion(int index, QueryCompletionAction action) {
            return new QueryCompletion(index, action);
        }
    }

    private HttpServletRequest createRequestFrom(String remoteHost) {
        return createGetRequest().withRemoteHost(remoteHost);
    }

    @Test
    void afterMultipleQueriesReceived_clearRemovesThem() {
        receiveQueries(3);
        ExporterQueries.clear();

        assertThat(ExporterQueries.getQueries(), empty());
    }

    @Test
    void whenQueryMarkedCompleted_reportTimeToComplete() {
        HttpServletRequest request = createRequestFrom("host1");
        ExporterQueries.addQuery(request);
        clock.incrementSeconds(1);
        ExporterQueries.completeQuery(request);

        assertThat(ExporterQueries.getQueryReport(), hasItem(containsString("in 1000 msec")));

    }

    @Test
    void whenQueryIncompleteButNotStuck_reportIt() {
        receiveQueries(1, completion(0, QueryCompletionAction.NONE));

        clock.incrementSeconds(QUERY_TIMEOUT_SECONDS / 2);

        assertThat(ExporterQueries.getQueryReport(), not(hasItem(containsString("STUCK"))));
    }

    @Test
    void whenQueryIsStuck_reportIt() {
        receiveQueries(1, completion(0, QueryCompletionAction.NONE));

        clock.incrementSeconds(QUERY_TIMEOUT_SECONDS);

        assertThat(ExporterQueries.getQueryReport(), hasItem(containsString("STUCK")));
    }

    @Test
    void reportTwoIncompleteQueries_asNotOverlapping() {
        ExporterQuery query1 = createQueryFrom("host1");
        clock.incrementSeconds(1);
        ExporterQuery query2 = createQueryFrom("host2");

        assertThat(query1.overlaps(query2), is(false));
    }

    @Test
    void reportQueriesOverlap_ifSecondStartsBetweenStartAndEndOfFirst() {
        ExporterQuery query1 = createQueryFrom("host1");
        clock.incrementSeconds(1);
        ExporterQuery query2 = createQueryFrom("host2");
        clock.incrementSeconds(1);
        query1.complete();

        assertThat(query1.overlaps(query2), is(true));
    }

    @Test
    void reportQueriesOverlap_ifFirstStartsBetweenStartAndEndOfSecond() {
        ExporterQuery query1 = createQueryFrom("host1");
        clock.incrementSeconds(1);
        ExporterQuery query2 = createQueryFrom("host2");
        clock.incrementSeconds(1);
        query1.complete();

        assertThat(query2.overlaps(query1), is(true));
    }

    @Test
    void whenQueryNotCompletedAndTimeSinceNotEverLimit_isNotMarkedAsStuck() {
        ExporterQuery query = createQueryFrom("host1");

        assertThat(query.isStuck(), is(false));
    }

    @Test
    void whenQueryNotCompletedAndTimeSinceReceivedOverLimit_isMarkedAsStuck() {
        ExporterQuery query = createQueryFrom("host1");
        clock.incrementSeconds(QUERY_TIMEOUT_SECONDS);

        assertThat(query.isStuck(), is(true));
    }

    @Test
    void whenQueryCompletedAndTimeSinceReceivedOverLimit_isNotMarkedAsStuck() {
        ExporterQuery query = createQueryFrom("host1");
        clock.incrementSeconds(QUERY_TIMEOUT_SECONDS);
        query.complete();

        assertThat(query.isStuck(), is(false));
    }

    private ExporterQuery createQueryFrom(String host) {
        return new ExporterQuery(createRequestFrom(host));
    }

    @Test
    void whenMoreThanAllowedQueriesReceived_pruneThem() {
        receiveQueries(NUM_QUERIES);

        final int firstHost = NUM_QUERIES - MAX_QUERIES + 1;
        assertThat(getQueryHosts(), hasItems(getHostMatchers(firstHost, NUM_QUERIES)));
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private Matcher<String>[] getHostMatchers(int firstHost, int lastHost) {
        Matcher<?>[] matchers = IntStream.rangeClosed(firstHost, lastHost)
                .mapToObj(String::valueOf)
                .map(this::toHostName)
                .map(Matchers::equalTo)
                .toArray(Matcher[]::new);
        return (Matcher<String>[]) matchers;
    }

    private String toHostName(String i) {
        return "host" + i;
    }

    @Test
    void recordTimeOfFirstQuery() {
        receiveQueries(NUM_QUERIES);

        assertThat(ExporterQueries.getQueryHeader(), containsString(INSTANT_FORMATTER.format(startInstant)));
    }

    @Test
    void reportNumberOfQueriesReceived() {
        receiveQueries(NUM_QUERIES);

        assertThat(ExporterQueries.getQueryHeader(), containsString(Integer.toString(NUM_QUERIES)));
    }

    @Test
    void doNotPruneActiveRequests() {
        receiveQueries(NUM_QUERIES, completion(0, QueryCompletionAction.NONE));

        assertThat(getQueryHosts(), hasItem("host1"));
        assertThat(getQueryHosts(), not(hasItem("host6")));
    }

    @Test
    void doNotPruneOverlappingQueries() {
        receiveQueries(NUM_QUERIES, completion(2, QueryCompletionAction.OVERLAP));

        assertThat(getQueryHosts(), hasItem("host3"));
        assertThat(getQueryHosts(), hasItem("host4"));
    }

    @Test
    void doNotPruneQueriesCloseToStuckQuery() {
        receiveQueries(4*NUM_QUERIES,
                completion(10, QueryCompletionAction.NONE),
                completion(20, QueryCompletionAction.NONE));

        assertThat(getQueryHosts(), not(hasItem("host4")));
        assertThat(getQueryHosts(), hasItem("host8"));
        assertThat(getQueryHosts(), hasItem("host9"));
        assertThat(getQueryHosts(), hasItem("host12"));
        assertThat(getQueryHosts(), hasItem("host19"));
    }

    @Test
    void whenMoreThanMaximumActiveQueries_keepThem() {
        QueryCompletion[] queryCompletions = IntStream.range(0, NUM_QUERIES).boxed().map(i -> completion(i, QueryCompletionAction.NONE)).toArray(QueryCompletion[]::new);
        receiveQueries(NUM_QUERIES, queryCompletions);

        assertThat(ExporterQueries.getQueries(), hasSize(NUM_QUERIES));
    }
}


