package com.oracle.wls.exporter;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
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
