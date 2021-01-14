// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * A class to provide the "items" potion of the json REST query.
 *
 * @author Russell Gold
 */
@SuppressWarnings("rawtypes")
class ItemHolder {
    ItemHolder(Map... maps) {
        items.addAll(Arrays.asList(maps));
    }

    @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private final ArrayList<Map> items = new ArrayList<>();

}
