// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ExporterQuery {
    static final long QUERY_TIMEOUT_SECONDS = 4;
    static final DateTimeFormatter INSTANT_FORMATTER
            = DateTimeFormatter.ofPattern("kk:mm:ss.SSS zzz").withZone(ZoneId.of("UTC"));

    @SuppressWarnings("FieldMayBeFinal") // not final, to support unit tests
    static private Clock clock = Clock.systemUTC();

    private final HttpServletRequest request;
    private final Instant queryReceivedAt;
    private Instant completedAt;

    ExporterQuery(HttpServletRequest request) {
        this.request = request;
        this.queryReceivedAt = clock.instant();
    }

    boolean overlaps(ExporterQuery other) {
        return spansStartOf(other)  || other.spansStartOf(this);
    }

    private boolean spansStartOf(ExporterQuery other) {
        return queryReceivedAt.isBefore(other.queryReceivedAt)
                && completedAt != null
                && completedAt.isAfter(other.queryReceivedAt);
    }

    void complete() {
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
        sb.append("Request from ").append(request.getRemoteHost());
        sb.append(" received at ").append(getReceivedTimeAsString());
        if (isStuck())
            sb.append(" and now STUCK");
        else if (completedAt != null) {
            Duration between = Duration.between(this.queryReceivedAt, completedAt);
            sb.append(" and completed in ").append(between.toMillis()).append(" msec");
        }
        return sb.toString();
    }

    String getReceivedTimeAsString() {
        return INSTANT_FORMATTER.format(queryReceivedAt);
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
}
