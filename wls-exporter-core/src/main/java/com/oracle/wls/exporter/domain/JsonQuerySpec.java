// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A class which Gson can convert to a JSON string for a WLS REST query. The REST API specifies that each
 * node is to be represented by an array of links, and array of values to retrieve,
 * and a map of nested nodes. This class always generates requests without links.
 *
 * @author Russell Gold
 */
@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
class JsonQuerySpec {
    private final String[] links = new String[0];
    private ArrayList<String> fields = null;
    private Map<String,JsonQuerySpec> children = null;

    /**
     * Specifies the name of any mbean values which should be retrieved.
     * @param newFields the field names to add to any previous defined
     */
    void addFields(String ... newFields) {
        if (fields == null) fields = new ArrayList<>();
        fields.addAll(Arrays.asList(newFields));
    }

    /**
     * Specifies a query for nested mbeans.
     * @param name the name of the nested mbean collection
     * @param child the query for the nested mbean collection
     */
    void addChild(String name, JsonQuerySpec child) {
        if (children == null)
            children = new HashMap<>();
        children.put(name, child);
    }
}
