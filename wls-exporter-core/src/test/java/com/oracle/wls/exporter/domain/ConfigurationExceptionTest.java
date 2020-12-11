// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class ConfigurationExceptionTest {
    @Test
    public void afterAddMultipleContexts_messageContainsFullContext() {
        ConfigurationException exception = new ConfigurationException("Something went wrong");

        exception.addContext("second");
        exception.addContext("first");

        assertThat(exception.getMessage(), equalTo("Something went wrong at first.second"));
    }
}
