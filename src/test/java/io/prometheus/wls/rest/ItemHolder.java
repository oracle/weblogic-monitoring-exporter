package io.prometheus.wls.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

class ItemHolder {
    ItemHolder(Map... maps) {
        items.addAll(Arrays.asList(maps));
    }
    private ArrayList<Map> items = new ArrayList<>();

    private void addMap(Map m) {
        items.add(m);
    }
}
