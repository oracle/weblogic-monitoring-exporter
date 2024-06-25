// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ExporterQueries {
    static final int MAX_QUERIES = 15;

    private static final List<ExporterQuery> queries = new ArrayList<>();
    static final int NUM_QUERIES_BEFORE_STUCK_QUERY_TO_RETAIN = 5;
    private static String initialQueryTime = null;
    private static long numQueriesReceived = 0;

    /**
     * Returns a header for the query report.
     */
    public synchronized static String getQueryHeader() {
        return "Exporter version " + LiveConfiguration.getVersionString() +
                "\nQueries received: " + numQueriesReceived +
                "\nqueries currently in queue: " + queries.size() +
                "\nInitial query received at " + initialQueryTime + "\n\n";
    }

    /**
     * Returns a collection of strings that describe the received queries
     */
    public synchronized static List<String> getQueryReport() {
        return queries.stream().map(ExporterQuery::toString).collect(Collectors.toList());
    }

    /**
     * Adds a new query to the list.
     * @param request the GET request from the client
     */
    public synchronized static ExporterQuery addQuery(HttpServletRequest request) {
        ExporterQuery query = new ExporterQuery(request);
        if (queries.isEmpty()) {
            initialQueryTime = query.getReceivedTimeAsString();
            numQueriesReceived = 0;
        }

        Iterator<ExporterQuery> each = queries.listIterator();
        while (queries.size() >= MAX_QUERIES && each.hasNext()) {
            ExporterQuery next = each.next();
            if (isRemovable(next)) {
                each.remove();
                break;
            }
        }

        numQueriesReceived++;
        queries.add(query);
        query.setIndex(numQueriesReceived);

        return query;
    }

    private static boolean isRemovable(ExporterQuery testQuery) {
        return testQuery.isComplete() && !overlapsAnother(testQuery) && !closeToStuckThread(testQuery);
    }

    private static boolean closeToStuckThread(ExporterQuery testQuery) {
        ExporterQuery firstStuckQuery = queries.stream().filter(ExporterQuery::isStuck).findFirst().orElse(null);
        if (firstStuckQuery == null) {
            return false;
        } else {
            final long distanceToStuckThread = firstStuckQuery.getIndex() - testQuery.getIndex();
            return (0 < distanceToStuckThread && distanceToStuckThread <= NUM_QUERIES_BEFORE_STUCK_QUERY_TO_RETAIN);
        }
    }

    private static boolean overlapsAnother(ExporterQuery testQuery) {
        for (ExporterQuery query : queries) {
            if (!query.equals(testQuery) && testQuery.overlaps(query))
                return true;
        }
        return false;
    }

    /**
     * Indicates that the specified query has been completed, meaning that a response has been received from the
     * REST API and will be formatted and returned to the client.
     * @param request the original GET request
     */
    public synchronized static void completeQuery(HttpServletRequest request) {
        queries.stream()
                .filter(q-> q.containsRequest(request)).findFirst()
                .ifPresent(ExporterQuery::complete);
    }


    // called from unit tests only

    public static List<ExporterQuery> getQueries() {
        return Collections.unmodifiableList(queries);
    }

    public static void clear() {
        queries.clear();
    }
}
