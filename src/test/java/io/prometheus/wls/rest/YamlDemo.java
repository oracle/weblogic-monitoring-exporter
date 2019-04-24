package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.prometheus.wls.rest.domain.ExporterConfig;
import io.prometheus.wls.rest.domain.MBeanSelector;

import java.io.ByteArrayInputStream;

import static io.prometheus.wls.rest.DemoInputs.*;

/**
 * @author Russell Gold
 */
public class YamlDemo {

    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        String yamlString = YAML_STRING3;
        System.out.println("The following configuration:\n" + yamlString);
        ExporterConfig exporterConfig = ExporterConfig.loadConfig(new ByteArrayInputStream(yamlString.getBytes()));

        System.out.println("Generates the query:");
        MBeanSelector selector = exporterConfig.getQueries()[0];
        System.out.println(selector.getPrintableRequest());

        System.out.println();
        String response = compressedJsonForm(RESPONSE);
        System.out.println("The response\n" + response + "\nwill be transformed into the following metrics:");

        exporterConfig.scrapeMetrics(selector, getJsonResponse(response)).
                forEach((name, value) -> System.out.printf("  %s %s%n", name, value));
    }

    private static JsonObject getJsonResponse(String jsonString) {
        return new JsonParser().parse(jsonString).getAsJsonObject();
    }

}
