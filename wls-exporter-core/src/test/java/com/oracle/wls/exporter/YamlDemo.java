// Copyright (c) 2017, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.gson.JsonObject;
import com.oracle.wls.exporter.domain.ExporterConfig;
import com.oracle.wls.exporter.domain.MBeanSelector;

import static com.google.gson.JsonParser.parseString;
import static com.oracle.wls.exporter.DemoInputs.*;

/**
 * @author Russell Gold
 */
public class YamlDemo {

    public static void main(String... args) throws IOException {
        String yamlString = YAML_STRING;
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
        return parseString(jsonString).getAsJsonObject();
    }

}
