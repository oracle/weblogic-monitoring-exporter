// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class ExporterQueries {
    static final int MAX_QUERIES = 6;

    private static final List<String> queries = new ArrayList<>();

    @SuppressWarnings("FieldMayBeFinal") // not final, to support unit tests
    private static Clock clock = Clock.systemUTC();

    public static List<String> getQueries() {
        return queries;
    }

    public static void addQuery(HttpServletRequest request) {
        while(queries.size() >= MAX_QUERIES) queries.remove(0);

        queries.add(ExporterQueryFormatter.format(request.getRemoteHost(), clock.instant()));
    }

    public static void clear() {
        queries.clear();
    }
}
