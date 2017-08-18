package io.prometheus.wls.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * A class to provide the "items" potion of the json REST query.
 */
class ItemHolder {
    ItemHolder(Map... maps) {
        items.addAll(Arrays.asList(maps));
    }

    @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private ArrayList<Map> items = new ArrayList<>();

}
