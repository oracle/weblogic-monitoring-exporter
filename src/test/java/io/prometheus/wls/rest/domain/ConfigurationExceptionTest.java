package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
@SuppressWarnings("ThrowableNotThrown")
public class ConfigurationExceptionTest {
    @Test
    public void afterAddMultipleContexts_messageContainsFullContext() throws Exception {
        ConfigurationException exception = new ConfigurationException("Something went wrong");

        exception.addContext("second");
        exception.addContext("first");

        assertThat(exception.getMessage(), equalTo("Something went wrong at first.second"));
    }
}
