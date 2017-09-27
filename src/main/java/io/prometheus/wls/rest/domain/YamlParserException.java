package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import org.yaml.snakeyaml.scanner.ScannerException;

/**
 * An exception thrown when there is an error parsing the YAML.
 */
public class YamlParserException extends ConfigurationException {
    private ScannerException scannerException;

    public YamlParserException(ScannerException scannerException) {
        super(BAD_YAML_FORMAT);
        this.scannerException = scannerException;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + '\n' + scannerException.getMessage();
    }
}
