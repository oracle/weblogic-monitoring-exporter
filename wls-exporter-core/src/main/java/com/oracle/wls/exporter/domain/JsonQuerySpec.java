// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A class which Gson can convert to a JSON string for a WLS REST query. The REST API specifies that each
 * node is to be represented by an array of links, and array of values to retrieve,
 * and a map of nested nodes. This class always generates requests without links.
 *
 * @author Russell Gold
 */
@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
class JsonQuerySpec {
    private List<String> fields = null;
    private Map<String, JsonQuerySpec> children = null;
    private String keyName = null;
    private List<String> selectedKeys = null;
    private List<String> excludeFields = null;

    JsonQuerySpec asTopLevel() {
        addFields();
        return this;
    }

    /**
     * Specifies the name of any mbean values which should be retrieved.
     * @param newFields the field names to add to any previous defined
     */
    void addFields(String... newFields) {
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

    void setFilter(String keyName, Set<String> selectedKeys) {
        this.keyName = keyName;
        this.selectedKeys = new ArrayList<>(selectedKeys);
    }

    String toJson(Gson gson) {
        return gson.toJson(toJsonObject());
    }

    JsonObject toJsonObject() {
        final JsonObject result = new JsonObject();
        
        result.add("links", new JsonArray());
        if (fields != null) result.add("fields", asStringArray(fields));
        if (excludeFields != null) result.add("excludeFields", asStringArray(excludeFields));
        if (keyName != null) result.add(keyName, asStringArray(selectedKeys));
        if (children != null) asChildObject(result);

        return result;
    }

    JsonArray asStringArray(List<String> values) {
        final JsonArray result = new JsonArray();
        values.forEach(result::add);
        return result;
    }

    private void asChildObject(JsonObject result) {
        final JsonObject nesting = new JsonObject();
        result.add("children", nesting);
        for (Map.Entry<String, JsonQuerySpec> entry : children.entrySet()) {
            nesting.add(entry.getKey(), entry.getValue().toJsonObject());
        }
    }

    public void excludeField(String fieldName) {
        if (excludeFields == null) excludeFields = new ArrayList<>();
        excludeFields.add(fieldName);
    }
}
