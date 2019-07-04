package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import org.yaml.snakeyaml.scanner.ScannerException;

/**
 * An exception thrown when there is an error parsing the YAML.
 *
 * @author Russell Gold
 */
public class YamlParserException extends ConfigurationException {
    private ScannerException scannerException;

    YamlParserException(ScannerException scannerException) {
        super(BAD_YAML_FORMAT);
        this.scannerException = scannerException;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + '\n' + scannerException.getMessage();
    }
}
