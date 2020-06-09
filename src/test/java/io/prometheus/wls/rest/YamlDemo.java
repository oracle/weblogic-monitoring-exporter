package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.prometheus.wls.rest.domain.ExporterConfig;
import io.prometheus.wls.rest.domain.MBeanSelector;

import static io.prometheus.wls.rest.DemoInputs.RESPONSE;
import static io.prometheus.wls.rest.DemoInputs.compressedJsonForm;

/**
 * @author Russell Gold
 */
public class YamlDemo {

    public static void main(String... args) throws IOException {
        String yamlString = String.join("\n", Files.readAllLines(Paths.get("/Users/rgold/Desktop/mohit.yml")));
//        String yamlString = YAML_STRING3;
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
