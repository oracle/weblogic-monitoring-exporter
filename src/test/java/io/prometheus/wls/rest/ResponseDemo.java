package io.prometheus.wls.rest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;

import java.util.Map;

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
