// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExporterQuery {
    static final long QUERY_TIMEOUT_SECONDS = 4;
    static final DateTimeFormatter INSTANT_FORMATTER
            = DateTimeFormatter.ofPattern("kk:mm:ss.SSS zzz").withZone(ZoneId.of("UTC"));

    @SuppressWarnings("FieldMayBeFinal") // not final, to support unit tests
    static private Clock clock = Clock.systemUTC();

    private final HttpServletRequest request;
    private final Instant queryReceivedAt;
    private Instant startedAt;
    private Instant completedAt;
    private final List<String> comments = new ArrayList<>();
    private final List<RestCall> restCalls = new ArrayList<>();
    private long index;

    ExporterQuery(HttpServletRequest request) {
        this.request = request;
        this.queryReceivedAt = clock.instant();
    }

    public void releasingSemaphore() {
        addComment("released semaphore");
    }

    private void addComment(String comment) {
//        comments.add("\n" + "request " + index + " " + asString(clock.instant()) + ": " + comment);
    }

    public void proceedingAfterWait() {
        addComment("proceeding");
        startedAt = clock.instant();
    }

    public void makingRestCall() {
        restCalls.add(new RestCall(clock.instant()));
    }

    public void restCallReceived() {
        restCalls.get(restCalls.size()-1).setCompleted(clock.instant());
    }

    public void restCallAborted(Throwable throwable) {
        restCalls.get(restCalls.size()-1).setThrowable(throwable);
    }

    public long getIndex() {
        return index;
    }

    static class RestCall {
        Instant started;
        Instant completed;
        Throwable throwable;

        public RestCall(Instant started) {
            this.started = started;
        }

        public void setCompleted(Instant completed) {
            this.completed = completed;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("REST call made at ").append(asString(started));
            if (completed != null) {
                Duration between = Duration.between(started, completed);
                sb.append(" with duration ").append(between.toMillis()).append(" msec");
            } else if (throwable instanceof AuthenticationChallengeException) {
                sb.append(" and received authentication challenge");
            } else if (throwable != null) {
                sb.append( " but aborted with error ").append(throwable.getClass()).append(": ").append(throwable.getMessage());
            }
            return sb.toString();
        }
    }

    public void waitingForSemaphore() {
        addComment("waiting for semaphore");
    }

    boolean overlaps(ExporterQuery other) {
        return spansStartOf(other)  || other.spansStartOf(this);
    }

    private boolean spansStartOf(ExporterQuery other) {
        return startTime().isBefore(other.startTime())
                && completedAt != null
                && completedAt.isAfter(other.startTime());
    }

    private Instant startTime() {
        return Optional.ofNullable(startedAt).orElse(queryReceivedAt);
    }

    public void complete() {
        this.completedAt = clock.instant();
    }

    boolean containsRequest(HttpServletRequest request) {
        return this.request.equals(request);
    }

    HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Request ").append(index).append(" from ").append(request.getRemoteHost());
        sb.append(" received at ").append(asString(queryReceivedAt));
        if (startedAt != null) sb.append(" started at ").append(asString(startedAt));
        sb.append("\n rest calls: ").append(restCalls.stream().map(RestCall::toString).collect(Collectors.joining(", ")));
        if (isStuck())
            sb.append("\n and now STUCK");
        else if (completedAt != null) {
            Duration between = Duration.between(queryReceivedAt, completedAt);
            sb.append("\n and completed in ").append(between.toMillis()).append(" msec");
        }
        comments.forEach(sb::append);
        return sb.toString();
    }

    String getReceivedTimeAsString() {
        return asString(queryReceivedAt);
    }

    private static String asString(Instant instant) {
        return INSTANT_FORMATTER.format(instant);
    }

    public boolean isStuck() {
        if (completedAt != null) return false;
        Instant queryReceivedAt = this.queryReceivedAt;
        return Duration.between(queryReceivedAt, clock.instant()).getSeconds() >= QUERY_TIMEOUT_SECONDS;
    }

    /**
     * Returns true if this query has completed.
     */
    boolean isComplete() {
        return completedAt != null;
    }

    public void setIndex(long index) {
        this.index = index;
    }
}
