package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;

import java.util.Map;

/**
 * @author Russell Gold
 */
public class ResponseDemo {

    public static void main(String... args) {
        Map<String, Object> map3 = ImmutableMap.of("deploymentState", 2, "name", "EjbStatusBean", "type", "EJBComponentRuntime");
        ItemHolder items2 = new ItemHolder(map3);
        Map<String, Object> map1 = ImmutableMap.of("internal", "false", "name", "mbeans", "componentRuntimes", items2);
        ItemHolder items0 = new ItemHolder(map1);

        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(
                ImmutableMap.of("name", "ejb30flexadmin", "applicationRuntimes", items0)));
    }

}
