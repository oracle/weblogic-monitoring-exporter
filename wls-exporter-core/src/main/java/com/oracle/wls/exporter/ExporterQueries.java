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
    private static String initialQueryTime = null;
    private static long numQueriesReceived = 0;

    /**
     * Returns a collection of strings that describe the received queries
     */
    public static List<String> getQueryReport() {
        return queries.stream().map(ExporterQuery::toString).collect(Collectors.toList());
    }

    public static List<ExporterQuery> getQueries() {
        return Collections.unmodifiableList(queries);
    }

    public static void addQuery(HttpServletRequest request) {
        ExporterQuery query = new ExporterQuery(request);
        if (queries.size() == 0) {
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
    }

    private static boolean isRemovable(ExporterQuery testQuery) {
        return testQuery.isComplete() && !overlapsAnother(testQuery);
    }

    private static boolean overlapsAnother(ExporterQuery testQuery) {
        for (ExporterQuery query : queries) {
            if (!query.equals(testQuery) && testQuery.overlaps(query))
                return true;
        }
        return false;
    }


    public static void clear() {
        queries.clear();
    }

    public static void completeQuery(HttpServletRequest request) {
        queries.stream()
                .filter(q-> q.containsRequest(request)).findFirst()
                .ifPresent(ExporterQuery::complete);
    }

    public static String getQueryHeader() {
        return "Queries received: " + numQueriesReceived +
                "\nInitial query received at " + initialQueryTime + "\n\n";
    }

}
