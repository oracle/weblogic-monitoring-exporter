// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oracle.wls.exporter.domain.ExporterConfig;
import com.oracle.wls.exporter.domain.MBeanSelector;

import static com.oracle.wls.exporter.DemoInputs.RESPONSE;
import static com.oracle.wls.exporter.DemoInputs.compressedJsonForm;

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
